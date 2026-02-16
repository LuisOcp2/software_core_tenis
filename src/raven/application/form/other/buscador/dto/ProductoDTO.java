package raven.application.form.other.buscador.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/* DTO para representar un producto en el buscador
    *@author CrisDEV
    *@version 2.1 - Ajustado sin ubicacion_bodega/ubicacion_tienda
    */

public class ProductoDTO {
    // Datos principales del producto
    private Integer idProducto;
    private String codigoModelo;
    private String nombre;
    private String genero;
    private byte[] imagenPrincipal; // Imagen del producto para mostrar en el buscador
    
    // Lista de variantes asociadas (carga lazy si es necesario)
    private List<VarianteDTO> variantes;
    
    // Constructor por defecto
    public ProductoDTO() {
        this.variantes = new ArrayList<>();
    }

    /**
     * Constructor completo para facilitar la creación desde ResultSet
     */
    public ProductoDTO(Integer idProducto, String codigoModelo, String nombre, 
                       String genero, byte[] imagenPrincipal) {
        this.idProducto = idProducto;
        this.codigoModelo = codigoModelo;
        this.nombre = nombre;
        this.genero = genero;
        this.imagenPrincipal = imagenPrincipal;
        this.variantes = new ArrayList<>();
    }
    
    // Getters y Setters
    
    public Integer getIdProducto() {
        return idProducto;
    }
    
    public void setIdProducto(Integer idProducto) {
        this.idProducto = idProducto;
    }
    
    public String getCodigoModelo() {
        return codigoModelo;
    }
    
    public void setCodigoModelo(String codigoModelo) {
        this.codigoModelo = codigoModelo;
    }
    
    public String getNombre() {
        return nombre;
    }
    
    public void setNombre(String nombre) {
        this.nombre = nombre;
    }
    
    public String getGenero() {
        return genero;
    }
    
    public void setGenero(String genero) {
        this.genero = genero;
    }
    
    public byte[] getImagenPrincipal() {
        return imagenPrincipal;
    }
    
    public void setImagenPrincipal(byte[] imagenPrincipal) {
        this.imagenPrincipal = imagenPrincipal;
    }
    
    public List<VarianteDTO> getVariantes() {
        return variantes;
    }
    
    public void setVariantes(List<VarianteDTO> variantes) {
        this.variantes = variantes;
    }
    
    /**
     * Agrega una variante a la lista
     * @param variante La variante a agregar
     */
    public void agregarVariante(VarianteDTO variante) {
        if (this.variantes == null) {
            this.variantes = new ArrayList<>();
        }
        this.variantes.add(variante);
    }
    
    /**
     * Retorna si el producto tiene variantes cargadas
     * @return true si tiene variantes
     */
    public boolean tieneVariantes() {
        return variantes != null && !variantes.isEmpty();
    }
    
    /**
     * Retorna la cantidad de variantes
     * @return número de variantes
     */
    public int getCantidadVariantes() {
        return variantes != null ? variantes.size() : 0;
    }
    
    @Override
    public String toString() {
        return String.format("ProductoDTO[id=%d, codigo=%s, nombre=%s, genero=%s, variantes=%d]",
                idProducto, codigoModelo, nombre, genero, getCantidadVariantes());
    }
}
