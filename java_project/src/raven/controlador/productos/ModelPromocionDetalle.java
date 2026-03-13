package raven.controlador.productos;

import java.time.LocalDateTime;

/**
 *
 * @author CrisDEV
 * Modelo para la tabla promociones_detalle
 * Define los ámbitos donde aplica cada promoción
 */
public class ModelPromocionDetalle {
    private int idDetalle;
    private int idPromocion;
    private String tipoAplicacion; // 'CATEGORIA', 'MARCA', 'PRODUCTO', 'ROL_USUARIO'
    
    // Un solo objetivo por fila (solo una de estas columnas debe tener valor)
    private Integer idCategoria;
    private Integer idMarca;
    private Integer idProducto;
    private Integer idUsuario; // ID específico del usuario para promociones ROL_USUARIO
    private String rolUsuario; // p.ej.: admin, vendedor, gerente, cliente
    
    private boolean activo;
    private LocalDateTime creadoEn;
    
    // Campos adicionales para mostrar información relacionada
    private String nombreCategoria;
    private String nombreMarca;
    private String nombreProducto;
    private String nombreUsuario;

    // Constructores
    public ModelPromocionDetalle() {}

    public ModelPromocionDetalle(int idDetalle, int idPromocion, String tipoAplicacion, boolean activo) {
        this.idDetalle = idDetalle;
        this.idPromocion = idPromocion;
        this.tipoAplicacion = tipoAplicacion;
        this.activo = activo;
    }

    // Constructor completo
    public ModelPromocionDetalle(int idDetalle, int idPromocion, String tipoAplicacion, 
                                Integer idCategoria, Integer idMarca, Integer idProducto, 
                                Integer idUsuario, String rolUsuario, boolean activo, LocalDateTime creadoEn) {
        this.idDetalle = idDetalle;
        this.idPromocion = idPromocion;
        this.tipoAplicacion = tipoAplicacion;
        this.idCategoria = idCategoria;
        this.idMarca = idMarca;
        this.idProducto = idProducto;
        this.idUsuario = idUsuario;
        this.rolUsuario = rolUsuario;
        this.activo = activo;
        this.creadoEn = creadoEn;
    }

    // Getters y Setters
    public int getIdDetalle() {
        return idDetalle;
    }
    public void setIdDetalle(int idDetalle) {
        this.idDetalle = idDetalle;
    }

    public int getIdPromocion() {
        return idPromocion;
    }
    public void setIdPromocion(int idPromocion) {
        this.idPromocion = idPromocion;
    }

    public String getTipoAplicacion() {
        return tipoAplicacion;
    }
    public void setTipoAplicacion(String tipoAplicacion) {
        this.tipoAplicacion = tipoAplicacion;
    }

    public Integer getIdCategoria() {
        return idCategoria;
    }
    public void setIdCategoria(Integer idCategoria) {
        this.idCategoria = idCategoria;
    }

    public Integer getIdMarca() {
        return idMarca;
    }
    public void setIdMarca(Integer idMarca) {
        this.idMarca = idMarca;
    }

    public Integer getIdProducto() {
        return idProducto;
    }
    public void setIdProducto(Integer idProducto) {
        this.idProducto = idProducto;
    }

    public Integer getIdUsuario() {
        return idUsuario;
    }
    public void setIdUsuario(Integer idUsuario) {
        this.idUsuario = idUsuario;
    }

    public String getRolUsuario() {
        return rolUsuario;
    }
    public void setRolUsuario(String rolUsuario) {
        this.rolUsuario = rolUsuario;
    }

    public boolean isActivo() {
        return activo;
    }
    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    public LocalDateTime getCreadoEn() {
        return creadoEn;
    }
    public void setCreadoEn(LocalDateTime creadoEn) {
        this.creadoEn = creadoEn;
    }

    // Campos adicionales para mostrar información relacionada
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

    public String getNombreUsuario() {
        return nombreUsuario;
    }
    public void setNombreUsuario(String nombreUsuario) {
        this.nombreUsuario = nombreUsuario;
    }

    // Métodos utilitarios
    public String getTipoAplicacionFormateado() {
        switch (tipoAplicacion) {
            case "CATEGORIA": return "Categoría";
            case "MARCA": return "Marca";
            case "PRODUCTO": return "Producto";
            case "ROL_USUARIO": return "Rol de Usuario";
            default: return tipoAplicacion;
        }
    }

    public String getObjetivoFormateado() {
        switch (tipoAplicacion) {
            case "CATEGORIA": 
                return nombreCategoria != null ? nombreCategoria : "Categoría ID: " + idCategoria;
            case "MARCA": 
                return nombreMarca != null ? nombreMarca : "Marca ID: " + idMarca;
            case "PRODUCTO": 
                return nombreProducto != null ? nombreProducto : "Producto ID: " + idProducto;
            case "ROL_USUARIO": 
                return rolUsuario != null ? rolUsuario : "Sin rol";
            default: 
                return "No definido";
        }
    }

    public String getEstadoFormateado() {
        return activo ? "Activo" : "Inactivo";
    }

    // Método para validar que solo hay un objetivo definido
    public boolean esObjetivoValido() {
        int count = 0;
        if (idCategoria != null) count++;
        if (idMarca != null) count++;
        if (idProducto != null) count++;
        if (idUsuario != null) count++;
        if (rolUsuario != null && !rolUsuario.trim().isEmpty()) count++;
        return count == 1;
    }

    @Override
    public String toString() {
        return String.format("ModelPromocionDetalle{id=%d, promocion=%d, tipo=%s, objetivo=%s}", 
                           idDetalle, idPromocion, tipoAplicacion, getObjetivoFormateado());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        ModelPromocionDetalle that = (ModelPromocionDetalle) obj;
        return idDetalle == that.idDetalle;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(idDetalle);
    }

    // Método para mostrar datos en la tabla
    public Object[] toTableRow(int rowNum) {
        return new Object[]{
            false,                          // SELECT (checkbox)
            rowNum,                         // N°
            getTipoAplicacionFormateado(),  // Tipo de Aplicación
            getObjetivoFormateado(),        // Objetivo
            getEstadoFormateado()           // Estado
        };
    }
}