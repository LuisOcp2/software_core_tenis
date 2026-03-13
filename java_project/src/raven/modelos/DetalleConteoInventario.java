package raven.modelos;

import java.util.Date;
import raven.controlador.productos.ModelProduct;

/**
 * Modelo que representa un detalle de conteo de inventario.
 * Contiene información de cada producto incluido en un conteo.
 */
public class DetalleConteoInventario {
    
    private int id;
    private int idConteo;
    private ModelProduct producto;
    private int stockSistema;
    private Integer stockContado;
    private Integer diferencia;
    private String estado; // pendiente, contado
    private Date fechaConteo;
    private Usuario usuarioContador;
    private String observaciones;
    private int idAjuste;
    private String tipoAjuste;
    private int cantidadAjuste;
    private String razonAjuste;
    
    public DetalleConteoInventario() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getIdConteo() {
        return idConteo;
    }

    public void setIdConteo(int idConteo) {
        this.idConteo = idConteo;
    }

    public ModelProduct getProducto() {
        return producto;
    }

    public void setProducto(ModelProduct producto) {
        this.producto = producto;
    }

    public int getStockSistema() {
        return stockSistema;
    }

    public void setStockSistema(int stockSistema) {
        this.stockSistema = stockSistema;
    }

    public Integer getStockContado() {
        return stockContado;
    }

    public void setStockContado(Integer stockContado) {
        this.stockContado = stockContado;
    }

    public Integer getDiferencia() {
        return diferencia;
    }

    public void setDiferencia(Integer diferencia) {
        this.diferencia = diferencia;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public Date getFechaConteo() {
        return fechaConteo;
    }

    public void setFechaConteo(Date fechaConteo) {
        this.fechaConteo = fechaConteo;
    }

    public Usuario getUsuarioContador() {
        return usuarioContador;
    }

    public void setUsuarioContador(Usuario usuarioContador) {
        this.usuarioContador = usuarioContador;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }

    public int getIdAjuste() {
        return idAjuste;
    }

    public void setIdAjuste(int idAjuste) {
        this.idAjuste = idAjuste;
    }

    public String getTipoAjuste() {
        return tipoAjuste;
    }

    public void setTipoAjuste(String tipoAjuste) {
        this.tipoAjuste = tipoAjuste;
    }

    public int getCantidadAjuste() {
        return cantidadAjuste;
    }

    public void setCantidadAjuste(int cantidadAjuste) {
        this.cantidadAjuste = cantidadAjuste;
    }

    public String getRazonAjuste() {
        return razonAjuste;
    }

    public void setRazonAjuste(String razonAjuste) {
        this.razonAjuste = razonAjuste;
    }
    
    /**
     * Convierte el detalle a un objeto para tabla de conteos
     * @param esContadoCajas Indica si el conteo es de cajas o pares
     * @return Array de objetos para añadir a la tabla
     */
    public Object[] toRowForConteoTable(boolean esContadoCajas) {
        return new Object[]{
            id,
            producto.getBarcode(),
            producto.getName(),
            stockSistema,
            stockContado,
            diferencia,
            estado,
            "Contar" // Botón para acción
        };
    }
    
    /**
     * Convierte el detalle a un objeto para tabla de ajustes
     * @return Array de objetos para añadir a la tabla
     */
    public Object[] toRowForAjusteTable() {
        return new Object[]{
            false, // Checkbox para aprobar
            producto.getBarcode(),
            producto.getName(),
            stockSistema,
            stockContado,
            diferencia,
            tipoAjuste,
            razonAjuste,
            
        };
    }
}