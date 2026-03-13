package raven.controlador.principal;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Modelo que representa el detalle (línea) de una compra externa.
 *
 * Mapea la tabla: compras_externas_detalles
 *
 * Relaciones: - compras_externas (id_compra_externa) - productos (id_producto)
 * - producto_variantes (id_variante)
 *
 * Principios aplicados: - Value Object parcial: cálculo automático de subtotal
 * - Encapsulamiento de lógica de cálculo
 *
 * @author CrisDEV
 * @version 1.0
 */
public class ModelCompraExternaDetalle {

    // ═══════════════════════════════════════════════════════════════════════════
    // ATRIBUTOS PRINCIPALES
    // ═══════════════════════════════════════════════════════════════════════════
    private Integer idDetalleCompraExterna;
    private Integer idCompraExterna;
    private Integer idProducto;
    private Integer idVariante;
    private int cantidad;
    private BigDecimal precioUnitario;
    private BigDecimal subtotal;
    private LocalDateTime fechaCreacion;

    // ═══════════════════════════════════════════════════════════════════════════
    // ATRIBUTOS DE DISPLAY (para UI)
    // ═══════════════════════════════════════════════════════════════════════════
    private String descripcionProducto;
    private String nombreTalla;
    private String nombreColor;
    private String sku;
    private BigDecimal precioVenta;  // Precio de venta sugerido
    private String ean;

    // ═══════════════════════════════════════════════════════════════════════════
    // CONSTRUCTORES
    // ═══════════════════════════════════════════════════════════════════════════
    public ModelCompraExternaDetalle() {
        this.cantidad = 1;
        this.precioUnitario = BigDecimal.ZERO;
        this.subtotal = BigDecimal.ZERO;
        this.fechaCreacion = LocalDateTime.now();
    }

    /**
     * Constructor con datos básicos.
     */
    public ModelCompraExternaDetalle(Integer idProducto, Integer idVariante,
            int cantidad, BigDecimal precioUnitario) {
        this();
        this.idProducto = idProducto;
        this.idVariante = idVariante;
        setCantidad(cantidad);
        setPrecioUnitario(precioUnitario);
    }

    /**
     * Constructor completo con descripción para display.
     */
    public ModelCompraExternaDetalle(Integer idProducto, Integer idVariante,
            String descripcion, String talla, String color,
            int cantidad, BigDecimal precioUnitario,
            BigDecimal precioVenta) {
        this(idProducto, idVariante, cantidad, precioUnitario);
        this.descripcionProducto = descripcion;
        this.nombreTalla = talla;
        this.nombreColor = color;
        this.precioVenta = precioVenta;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // BUILDER PATTERN
    // ═══════════════════════════════════════════════════════════════════════════
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private final ModelCompraExternaDetalle detalle = new ModelCompraExternaDetalle();

        public Builder idDetalle(Integer id) {
            detalle.idDetalleCompraExterna = id;
            return this;
        }

        public Builder compraExterna(Integer idCompra) {
            detalle.idCompraExterna = idCompra;
            return this;
        }

        public Builder producto(Integer idProducto) {
            detalle.idProducto = idProducto;
            return this;
        }

        public Builder variante(Integer idVariante) {
            detalle.idVariante = idVariante;
            return this;
        }

        public Builder cantidad(int cantidad) {
            detalle.setCantidad(cantidad);
            return this;
        }

        public Builder precioUnitario(BigDecimal precio) {
            detalle.setPrecioUnitario(precio);
            return this;
        }

        public Builder precioUnitario(double precio) {
            detalle.setPrecioUnitario(BigDecimal.valueOf(precio));
            return this;
        }

        public Builder descripcion(String descripcion) {
            detalle.descripcionProducto = descripcion;
            return this;
        }

        public Builder talla(String talla) {
            detalle.nombreTalla = talla;
            return this;
        }

        public Builder color(String color) {
            detalle.nombreColor = color;
            return this;
        }

        public Builder sku(String sku) {
            detalle.sku = sku;
            return this;
        }

        public Builder precioVenta(BigDecimal precioVenta) {
            detalle.precioVenta = precioVenta;
            return this;
        }

        public ModelCompraExternaDetalle build() {
            detalle.validar();
            return detalle;
        }
    }

    public String getEan() {
        return ean;
    }

    public void setEan(String ean) {
        this.ean = ean;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // GETTERS Y SETTERS CON VALIDACIÓN
    // ═══════════════════════════════════════════════════════════════════════════
    public Integer getIdDetalleCompraExterna() {
        return idDetalleCompraExterna;
    }

    public void setIdDetalleCompraExterna(Integer idDetalleCompraExterna) {
        this.idDetalleCompraExterna = idDetalleCompraExterna;
    }

    public Integer getIdCompraExterna() {
        return idCompraExterna;
    }

    public void setIdCompraExterna(Integer idCompraExterna) {
        this.idCompraExterna = idCompraExterna;
    }

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

    public int getCantidad() {
        return cantidad;
    }

    /**
     * Establece cantidad y recalcula subtotal automáticamente.
     */
    public void setCantidad(int cantidad) {
        if (cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor a cero");
        }
        this.cantidad = cantidad;
        recalcularSubtotal();
    }

    public BigDecimal getPrecioUnitario() {
        return precioUnitario;
    }

    /**
     * Establece precio unitario y recalcula subtotal automáticamente.
     */
    public void setPrecioUnitario(BigDecimal precioUnitario) {
        if (precioUnitario != null && precioUnitario.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("El precio no puede ser negativo");
        }
        this.precioUnitario = precioUnitario != null ? precioUnitario : BigDecimal.ZERO;
        recalcularSubtotal();
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    /**
     * Subtotal es calculado, no se debe establecer directamente. Este setter es
     * solo para mapeo desde BD.
     */
    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    // Getters y Setters para campos de display
    public String getDescripcionProducto() {
        return descripcionProducto;
    }

    public void setDescripcionProducto(String descripcionProducto) {
        this.descripcionProducto = descripcionProducto;
    }

    public String getNombreTalla() {
        return nombreTalla;
    }

    public void setNombreTalla(String nombreTalla) {
        this.nombreTalla = nombreTalla;
    }

    public String getNombreColor() {
        return nombreColor;
    }

    public void setNombreColor(String nombreColor) {
        this.nombreColor = nombreColor;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public BigDecimal getPrecioVenta() {
        return precioVenta;
    }

    public void setPrecioVenta(BigDecimal precioVenta) {
        this.precioVenta = precioVenta;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // MÉTODOS DE NEGOCIO
    // ═══════════════════════════════════════════════════════════════════════════
    /**
     * Recalcula el subtotal basado en cantidad y precio unitario.
     */
    private void recalcularSubtotal() {
        this.subtotal = precioUnitario.multiply(BigDecimal.valueOf(cantidad));
    }

    /**
     * Valida que el detalle tenga los campos requeridos.
     */
    public void validar() {
        StringBuilder errores = new StringBuilder();

        if (idProducto == null) {
            errores.append("- Producto es requerido\n");
        }
        if (idVariante == null) {
            errores.append("- Variante es requerida\n");
        }
        if (cantidad <= 0) {
            errores.append("- Cantidad debe ser mayor a cero\n");
        }
        if (precioUnitario == null || precioUnitario.compareTo(BigDecimal.ZERO) <= 0) {
            errores.append("- Precio unitario debe ser mayor a cero\n");
        }

        if (errores.length() > 0) {
            throw new IllegalStateException("Errores de validación:\n" + errores);
        }
    }

    /**
     * Calcula el margen de ganancia esperado.
     *
     * @return Margen como porcentaje, o null si no hay precio de venta
     */
    public BigDecimal calcularMargenPorcentaje() {
        if (precioVenta == null || precioVenta.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        if (precioUnitario == null || precioUnitario.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }

        // Margen = ((PrecioVenta - PrecioCompra) / PrecioCompra) * 100
        BigDecimal diferencia = precioVenta.subtract(precioUnitario);
        return diferencia
                .divide(precioUnitario, 4, java.math.RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }

    /**
     * Calcula la ganancia bruta esperada por este item.
     */
    public BigDecimal calcularGananciaBruta() {
        if (precioVenta == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal ventaTotal = precioVenta.multiply(BigDecimal.valueOf(cantidad));
        return ventaTotal.subtract(subtotal);
    }

    /**
     * Genera una descripción completa para mostrar en UI.
     */
    public String getDescripcionCompleta() {
        StringBuilder sb = new StringBuilder();

        if (descripcionProducto != null) {
            sb.append(descripcionProducto);
        }

        if (nombreTalla != null || nombreColor != null) {
            sb.append(" (");
            if (nombreTalla != null) {
                sb.append("Talla: ").append(nombreTalla);
            }
            if (nombreTalla != null && nombreColor != null) {
                sb.append(" - ");
            }
            if (nombreColor != null) {
                sb.append("Color: ").append(nombreColor);
            }
            sb.append(")");
        }

        return sb.toString();
    }

    @Override
    public String toString() {
        return String.format("%s x%d @ $%s = $%s",
                getDescripcionCompleta(),
                cantidad,
                precioUnitario,
                subtotal);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        ModelCompraExternaDetalle that = (ModelCompraExternaDetalle) obj;

        // Si tiene ID, comparar por ID
        if (idDetalleCompraExterna != null && that.idDetalleCompraExterna != null) {
            return idDetalleCompraExterna.equals(that.idDetalleCompraExterna);
        }

        // Si no, comparar por variante (único en una compra)
        return idVariante != null && idVariante.equals(that.idVariante)
                && idCompraExterna != null && idCompraExterna.equals(that.idCompraExterna);
    }

    @Override
    public int hashCode() {
        if (idDetalleCompraExterna != null) {
            return idDetalleCompraExterna.hashCode();
        }
        int result = idVariante != null ? idVariante.hashCode() : 0;
        result = 31 * result + (idCompraExterna != null ? idCompraExterna.hashCode() : 0);
        return result;
    }
}
