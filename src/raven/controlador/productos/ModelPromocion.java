package raven.controlador.productos;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 *
 * @author CrisDEV
 * Modelo para la tabla promociones
 * 
 */
public class ModelPromocion {
    private int idPromocion;//ID
    private String codigo; // Código único (cupón/etiqueta)
    private String nombre;//NOMBRE
    private String descripcion;//DESCRIPCION
    private String tipoDescuento; // 'PORCENTAJE' o 'MONTO_FIJO'
    private double valorDescuento;
    private LocalDateTime fechaInicio;
    private LocalDateTime fechaFin;
    private boolean activa;
    private Double minCompra;
    
    // Límites opcionales
    private Integer limiteUsoTotal;
    private Integer limiteUsoPorUsuario;
    
    // Auditoría
    private LocalDateTime creadoEn;
    private LocalDateTime actualizadoEn;
    
    // Campos adicionales para mostrar información relacionada
    private String nombreCategoria;
    private String nombreMarca;
    private String nombreProducto;

    // Constructores
    public ModelPromocion() {}

    public ModelPromocion(int idPromocion, String codigo, String nombre, String tipoDescuento, 
                         double valorDescuento, LocalDateTime fechaInicio, 
                         LocalDateTime fechaFin, boolean activa) {
        this.idPromocion = idPromocion;
        this.codigo = codigo;
        this.nombre = nombre;
        this.tipoDescuento = tipoDescuento;
        this.valorDescuento = valorDescuento;
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
        this.activa = activa;
    }

    // Constructor completo
    public ModelPromocion(int idPromocion, String codigo, String nombre, String descripcion, 
                         String tipoDescuento, double valorDescuento, 
                         LocalDateTime fechaInicio, LocalDateTime fechaFin, 
                         boolean activa, Double minCompra, Integer limiteUsoTotal, 
                         Integer limiteUsoPorUsuario, LocalDateTime creadoEn, 
                         LocalDateTime actualizadoEn) {
        this.idPromocion = idPromocion;
        this.codigo = codigo;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.tipoDescuento = tipoDescuento;
        this.valorDescuento = valorDescuento;
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
        this.activa = activa;
        this.minCompra = minCompra;
        this.limiteUsoTotal = limiteUsoTotal;
        this.limiteUsoPorUsuario = limiteUsoPorUsuario;
        this.creadoEn = creadoEn;
        this.actualizadoEn = actualizadoEn;
    }

    // Getters y Setters
    public int getIdPromocion() { 
        return idPromocion; 
    }
    public void setIdPromocion(int idPromocion) {
        this.idPromocion = idPromocion; 
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

    public String getDescripcion() {
        return descripcion; 
    }
    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion; 
    }

    public String getTipoDescuento() {
        return tipoDescuento; 
    }
    public void setTipoDescuento(String tipoDescuento) {
        this.tipoDescuento = tipoDescuento; 
    }

    public double getValorDescuento() {
        return valorDescuento; 
    }
    public void setValorDescuento(double valorDescuento) {
        this.valorDescuento = valorDescuento; 
    }

    public LocalDateTime getFechaInicio() {
        return fechaInicio; 
    }
    public void setFechaInicio(LocalDateTime fechaInicio) {
        this.fechaInicio = fechaInicio; 
    }

    public LocalDateTime getFechaFin() {
        return fechaFin; 
    }
    public void setFechaFin(LocalDateTime fechaFin) {
        this.fechaFin = fechaFin; 
    }

    public boolean isActiva() { 
        return activa; 
    }
    public void setActiva(boolean activa) { 
        this.activa = activa; 
    }

    public Double getMinCompra() {
        return minCompra; 
    }
    public void setMinCompra(Double minCompra) {
        this.minCompra = minCompra; 
    }

    public Integer getLimiteUsoTotal() {
        return limiteUsoTotal; 
    }
    public void setLimiteUsoTotal(Integer limiteUsoTotal) {
        this.limiteUsoTotal = limiteUsoTotal; 
    }

    public Integer getLimiteUsoPorUsuario() {
        return limiteUsoPorUsuario; 
    }
    public void setLimiteUsoPorUsuario(Integer limiteUsoPorUsuario) {
        this.limiteUsoPorUsuario = limiteUsoPorUsuario; 
    }

    public LocalDateTime getCreadoEn() {
        return creadoEn; 
    }
    public void setCreadoEn(LocalDateTime creadoEn) {
        this.creadoEn = creadoEn; 
    }

    public LocalDateTime getActualizadoEn() {
        return actualizadoEn; 
    }
    public void setActualizadoEn(LocalDateTime actualizadoEn) {
        this.actualizadoEn = actualizadoEn; 
    }

    // Getters y setters para campos adicionales
    public String getNombreCategoria() {
        return nombreCategoria;
    }
    public void setNombreCategoria(String nombreCategoria) {
        this.nombreCategoria = nombreCategoria;
    }

    public String getNombreMarca() {
        return nombreMarca;
    }
    public void setNombreMarca(String nombreMarca) {
        this.nombreMarca = nombreMarca;
    }

    public String getNombreProducto() {
        return nombreProducto;
    }
    public void setNombreProducto(String nombreProducto) {
        this.nombreProducto = nombreProducto;
    }

    // Métodos utilitarios
    public String getCodigoPromocion() {
        return codigo != null ? codigo : String.format("PROMO-%04d", idPromocion);
    }

    public String getTipoDescuentoFormateado() {
        return tipoDescuento.equals("PORCENTAJE") ? "Porcentaje" : "Monto Fijo";
    }

    public String getValorDescuentoFormateado() {
        if (tipoDescuento.equals("PORCENTAJE")) {
            return String.format("%.1f%%", valorDescuento);
        } else {
            return String.format("$%.2f", valorDescuento);
        }
    }

    public String getFechaInicioFormateada() {
        if (fechaInicio != null) {
            return fechaInicio.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        }
        return "";
    }

    public String getFechaFinFormateada() {
        if (fechaFin != null) {
            return fechaFin.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        }
        return "";
    }

    public String getEstadoFormateado() {
        return activa ? "Activa" : "Inactiva";
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
        ModelPromocion promocion = (ModelPromocion) obj;
        return idPromocion == promocion.idPromocion;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(idPromocion);
    }

    // Método para mostrar datos en la tabla
    // Columnas: "SELECT", "N°", "Nombre", "Código", "Tipo", "Valor", "Fecha Inicio", "Fecha Final", "Activo"
    public Object[] toTableRow(int rowNum) {
        return new Object[]{
            false,                          // SELECT (checkbox)
            rowNum,                         // N°
            this,                          // Nombre (objeto completo para renderizado personalizado)
            getCodigoPromocion(),          // Código
            getTipoDescuentoFormateado(),  // Tipo
            getValorDescuentoFormateado(), // Valor
            getFechaInicioFormateada(),    // Fecha Inicio
            getFechaFinFormateada(),       // Fecha Final
            getEstadoFormateado()          // Activo
        };
    }
}