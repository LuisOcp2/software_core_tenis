/*
 * ProductoTraspasoItem - VERSIÓN CORREGIDA
 * Corrige validaciones y manejo de datos
 */
package raven.clases.productos;

import java.math.BigDecimal;

public class ProductoTraspasoItem_1 {
    
    private Integer idProducto;
    private Integer idVariante;
    private String codigoProducto;
    private String nombreProducto;
    private String color;
    private String talla;
    private String tipo; // "caja" o "par"
    private Integer cantidadSolicitada;
    private Integer cantidadEnviada;
    private Integer cantidadRecibida;
    private BigDecimal precioUnitario;
    private Integer stockDisponible;
    private String observaciones;
    private String sku;
    private String ean;
    private Integer paresPorCaja;

    public ProductoTraspasoItem_1() {
        this.cantidadSolicitada = 0;
        this.cantidadEnviada = 0;
        this.cantidadRecibida = 0;
        this.stockDisponible = 0;
        this.precioUnitario = BigDecimal.ZERO;
        this.paresPorCaja = 24; // Valor por defecto
    }

    // Getters y Setters
    public Integer getIdProducto() {
        return idProducto;
    }

    public void setIdProducto(Integer idProducto) {
        this.idProducto = idProducto;
    }

    public Integer getIdVariante() {
        return idVariante;
    }

    public void setIdVariante(Integer idVariante) {
        this.idVariante = idVariante;
    }

    public String getCodigoProducto() {
        return codigoProducto;
    }

    public void setCodigoProducto(String codigoProducto) {
        this.codigoProducto = codigoProducto;
    }

    public String getNombreProducto() {
        return nombreProducto;
    }

    public void setNombreProducto(String nombreProducto) {
        this.nombreProducto = nombreProducto;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getTalla() {
        return talla;
    }

    public void setTalla(String talla) {
        this.talla = talla;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public Integer getCantidadSolicitada() {
        return cantidadSolicitada;
    }

    public void setCantidadSolicitada(Integer cantidadSolicitada) {
        this.cantidadSolicitada = cantidadSolicitada;
    }

    public Integer getCantidadEnviada() {
        return cantidadEnviada;
    }

    public void setCantidadEnviada(Integer cantidadEnviada) {
        this.cantidadEnviada = cantidadEnviada;
    }

    public Integer getCantidadRecibida() {
        return cantidadRecibida;
    }

    public void setCantidadRecibida(Integer cantidadRecibida) {
        this.cantidadRecibida = cantidadRecibida;
    }

    public BigDecimal getPrecioUnitario() {
        return precioUnitario;
    }

    public void setPrecioUnitario(BigDecimal precioUnitario) {
        this.precioUnitario = precioUnitario;
    }

    public Integer getStockDisponible() {
        return stockDisponible;
    }

    public void setStockDisponible(Integer stockDisponible) {
        this.stockDisponible = stockDisponible;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public String getEan() {
        return ean;
    }

    public void setEan(String ean) {
        this.ean = ean;
    }

    public Integer getParesPorCaja() {
        return paresPorCaja;
    }

    public void setParesPorCaja(Integer paresPorCaja) {
        this.paresPorCaja = paresPorCaja;
    }

    // MÉTODOS CORREGIDOS
    
    /**
     * MÉTODO CORREGIDO - Genera nombre completo con validaciones
     */
    public String getNombreCompleto() {
        StringBuilder nombre = new StringBuilder();
        
        if (nombreProducto != null && !nombreProducto.trim().isEmpty()) {
            nombre.append(nombreProducto);
        } else {
            nombre.append("Producto sin nombre");
        }
        
        // Agregar color y talla solo si están disponibles
        if (color != null && !color.trim().isEmpty() && 
            talla != null && !talla.trim().isEmpty() && 
            !color.equals("Color") && !talla.equals("Talla")) {
            nombre.append(" (").append(color).append(" - ").append(talla).append(")");
        } else if (color != null && !color.trim().isEmpty() && !color.equals("Color")) {
            nombre.append(" (").append(color).append(")");
        } else if (talla != null && !talla.trim().isEmpty() && !talla.equals("Talla")) {
            nombre.append(" (Talla ").append(talla).append(")");
        }
        
        return nombre.toString();
    }

    /**
     * MÉTODO CORREGIDO - Validación mejorada
     */
    public boolean esValido() {
        // Validaciones básicas
        if (idProducto == null || idProducto <= 0) {
            System.out.println("ERROR  ID de producto inválido: " + idProducto);
            return false;
        }
        
        if (nombreProducto == null || nombreProducto.trim().isEmpty()) {
            System.out.println("ERROR  Nombre de producto vacío");
            return false;
        }
        
        if (tipo == null || tipo.trim().isEmpty() || 
            (!tipo.equals("caja") && !tipo.equals("par"))) {
            System.out.println("ERROR  Tipo inválido: " + tipo);
            return false;
        }
        
        if (cantidadSolicitada == null || cantidadSolicitada <= 0) {
            System.out.println("ERROR  Cantidad solicitada inválida: " + cantidadSolicitada);
            return false;
        }
        
        if (precioUnitario == null || precioUnitario.compareTo(BigDecimal.ZERO) < 0) {
            System.out.println("ERROR  Precio unitario inválido: " + precioUnitario);
            return false;
        }
        
        // NOTA: idVariante puede ser null para productos sin variantes específicas
        // No es un error crítico
        
        return true;
    }

    /**
     * MÉTODO NUEVO - Calcular subtotal
     */
    public BigDecimal calcularSubtotal() {
        if (precioUnitario != null && cantidadSolicitada != null && cantidadSolicitada > 0) {
            return precioUnitario.multiply(BigDecimal.valueOf(cantidadSolicitada));
        }
        return BigDecimal.ZERO;
    }

    /**
     * MÉTODO NUEVO - Verificar si hay stock suficiente
     */
    public boolean hayStockSuficiente() {
        if (stockDisponible == null || cantidadSolicitada == null) {
            return false;
        }
        return stockDisponible >= cantidadSolicitada;
    }

    /**
     * MÉTODO NUEVO - Obtener información de stock
     */
    public String getInfoStock() {
        if (stockDisponible != null) {
            return "Stock disponible: " + stockDisponible + " " + (tipo != null ? tipo : "unidades");
        }
        return "Stock no disponible";
    }

    /**
     * MÉTODO NUEVO - Verificar si el producto tiene variantes
     */
    public boolean tieneVariantes() {
        return idVariante != null && idVariante > 0;
    }

    /**
     * MÉTODO NUEVO - Obtener descripción corta para debugging
     */
    public String getDescripcionCorta() {
        return String.format("ID:%d, %s, %d %s", 
                           idProducto != null ? idProducto : 0,
                           getNombreCompleto(),
                           cantidadSolicitada != null ? cantidadSolicitada : 0,
                           tipo != null ? tipo : "unidades");
    }

    /**
     * MÉTODO NUEVO - Clonar producto (útil para modificaciones)
     */
    public ProductoTraspasoItem_1 clonar() {
        ProductoTraspasoItem_1 clon = new ProductoTraspasoItem_1();
        clon.setIdProducto(this.idProducto);
        clon.setIdVariante(this.idVariante);
        clon.setCodigoProducto(this.codigoProducto);
        clon.setNombreProducto(this.nombreProducto);
        clon.setColor(this.color);
        clon.setTalla(this.talla);
        clon.setTipo(this.tipo);
        clon.setCantidadSolicitada(this.cantidadSolicitada);
        clon.setCantidadEnviada(this.cantidadEnviada);
        clon.setCantidadRecibida(this.cantidadRecibida);
        clon.setPrecioUnitario(this.precioUnitario);
        clon.setStockDisponible(this.stockDisponible);
        clon.setObservaciones(this.observaciones);
        clon.setSku(this.sku);
        clon.setEan(this.ean);
        clon.setParesPorCaja(this.paresPorCaja);
        return clon;
    }

    /**
     * MÉTODO NUEVO - Convertir cantidad entre tipos (caja ↔ par)
     */
    public int convertirCantidad(String tipoDestino) {
        if (cantidadSolicitada == null || cantidadSolicitada == 0) {
            return 0;
        }
        
        if (tipo.equals(tipoDestino)) {
            return cantidadSolicitada;
        }
        
        int paresXCaja = paresPorCaja != null ? paresPorCaja : 24;
        
        if (tipo.equals("caja") && tipoDestino.equals("par")) {
            return cantidadSolicitada * paresXCaja;
        } else if (tipo.equals("par") && tipoDestino.equals("caja")) {
            return cantidadSolicitada / paresXCaja;
        }
        
        return cantidadSolicitada;
    }

    @Override
    public String toString() {
        return getDescripcionCorta();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        ProductoTraspasoItem_1 that = (ProductoTraspasoItem_1) obj;
        
        if (!idProducto.equals(that.idProducto)) return false;
        if (idVariante != null ? !idVariante.equals(that.idVariante) : that.idVariante != null) return false;
        return tipo.equals(that.tipo);
    }

    @Override
    public int hashCode() {
        int result = idProducto.hashCode();
        result = 31 * result + (idVariante != null ? idVariante.hashCode() : 0);
        result = 31 * result + tipo.hashCode();
        return result;
    }
}
