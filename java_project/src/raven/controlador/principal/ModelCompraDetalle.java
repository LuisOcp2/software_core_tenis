package raven.controlador.principal;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Modelo que representa el detalle (línea) de una compra a proveedor.
 * 
 * Mapea la tabla: compra_detalles
 * 
 * Relaciones:
 * - compras (id_compra)
 * - productos (id_producto)
 * - producto_variantes (id_variante)
 * 
 * @author CrisDEV
 * @version 1.0
 */
public class ModelCompraDetalle {

    // ═══════════════════════════════════════════════════════════════════════════
    // ENUMS
    // ═══════════════════════════════════════════════════════════════════════════

    public enum TipoUnidad {
        PAR("par"),
        CAJA("caja");

        private final String valor;

        TipoUnidad(String valor) {
            this.valor = valor;
        }

        public String getValor() {
            return valor;
        }

        public static TipoUnidad fromString(String texto) {
            for (TipoUnidad t : values()) {
                if (t.valor.equalsIgnoreCase(texto)) {
                    return t;
                }
            }
            return PAR;
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // ATRIBUTOS PRINCIPALES
    // ═══════════════════════════════════════════════════════════════════════════

    private Integer idDetalleCompra;
    private Integer idCompra;
    private Integer idProducto;
    private Integer idVariante;
    private int cantidad;
    private TipoUnidad tipoUnidad;
    private BigDecimal precioUnitario;
    private BigDecimal subtotal;
    private String observaciones;
    private LocalDateTime fechaCreacion;

    // ═══════════════════════════════════════════════════════════════════════════
    // ATRIBUTOS DE DISPLAY (para UI)
    // ═══════════════════════════════════════════════════════════════════════════

    private String codigoModelo;
    private String nombreProducto;
    private String nombreTalla;
    private String nombreColor;
    private String sku;
    private String ean;
    private BigDecimal precioVenta;

    // ═══════════════════════════════════════════════════════════════════════════
    // CONSTRUCTORES
    // ═══════════════════════════════════════════════════════════════════════════

    public ModelCompraDetalle() {
        this.cantidad = 1;
        this.tipoUnidad = TipoUnidad.PAR;
        this.precioUnitario = BigDecimal.ZERO;
        this.subtotal = BigDecimal.ZERO;
        this.fechaCreacion = LocalDateTime.now();
    }

    /**
     * Constructor con datos básicos.
     */
    public ModelCompraDetalle(Integer idProducto, Integer idVariante,
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
    public ModelCompraDetalle(Integer idProducto, Integer idVariante,
            String nombreProducto, String talla, String color,
            int cantidad, TipoUnidad tipoUnidad, BigDecimal precioUnitario) {
        this(idProducto, idVariante, cantidad, precioUnitario);
        this.nombreProducto = nombreProducto;
        this.nombreTalla = talla;
        this.nombreColor = color;
        this.tipoUnidad = tipoUnidad;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // BUILDER PATTERN
    // ═══════════════════════════════════════════════════════════════════════════

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final ModelCompraDetalle detalle = new ModelCompraDetalle();

        public Builder idDetalle(Integer id) {
            detalle.idDetalleCompra = id;
            return this;
        }

        public Builder compra(Integer idCompra) {
            detalle.idCompra = idCompra;
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

        public Builder tipoUnidad(TipoUnidad tipo) {
            detalle.tipoUnidad = tipo;
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

        public Builder nombreProducto(String nombre) {
            detalle.nombreProducto = nombre;
            return this;
        }

        public Builder codigoModelo(String codigo) {
            detalle.codigoModelo = codigo;
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

        public Builder ean(String ean) {
            detalle.ean = ean;
            return this;
        }

        public Builder precioVenta(BigDecimal precioVenta) {
            detalle.precioVenta = precioVenta;
            return this;
        }

        public ModelCompraDetalle build() {
            detalle.validar();
            return detalle;
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // GETTERS Y SETTERS
    // ═══════════════════════════════════════════════════════════════════════════

    public Integer getIdDetalleCompra() {
        return idDetalleCompra;
    }

    public void setIdDetalleCompra(Integer idDetalleCompra) {
        this.idDetalleCompra = idDetalleCompra;
    }

    public Integer getIdCompra() {
        return idCompra;
    }

    public void setIdCompra(Integer idCompra) {
        this.idCompra = idCompra;
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

    public TipoUnidad getTipoUnidad() {
        return tipoUnidad;
    }

    public void setTipoUnidad(TipoUnidad tipoUnidad) {
        this.tipoUnidad = tipoUnidad != null ? tipoUnidad : TipoUnidad.PAR;
    }

    public void setTipoUnidadFromString(String tipo) {
        this.tipoUnidad = TipoUnidad.fromString(tipo);
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

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    // Getters y Setters para campos de display
    public String getCodigoModelo() {
        return codigoModelo;
    }

    public void setCodigoModelo(String codigoModelo) {
        this.codigoModelo = codigoModelo;
    }

    public String getNombreProducto() {
        return nombreProducto;
    }

    public void setNombreProducto(String nombreProducto) {
        this.nombreProducto = nombreProducto;
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

    public String getEan() {
        return ean;
    }

    public void setEan(String ean) {
        this.ean = ean;
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
        int unidades = (tipoUnidad == TipoUnidad.CAJA) ? (cantidad * 24) : cantidad;
        this.subtotal = precioUnitario.multiply(BigDecimal.valueOf(unidades));
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
     */
    public BigDecimal calcularMargenPorcentaje() {
        if (precioVenta == null || precioVenta.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        if (precioUnitario == null || precioUnitario.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }

        BigDecimal diferencia = precioVenta.subtract(precioUnitario);
        return diferencia
                .divide(precioUnitario, 4, java.math.RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }

    /**
     * Genera una descripción completa para mostrar en UI.
     */
    public String getDescripcionCompleta() {
        StringBuilder sb = new StringBuilder();

        if (nombreProducto != null) {
            sb.append(nombreProducto);
        }

        if (nombreTalla != null || nombreColor != null) {
            sb.append(" (");
            if (nombreTalla != null) {
                sb.append("T: ").append(nombreTalla);
            }
            if (nombreTalla != null && nombreColor != null) {
                sb.append(" / ");
            }
            if (nombreColor != null) {
                sb.append("C: ").append(nombreColor);
            }
            sb.append(")");
        }

        return sb.toString();
    }

    /**
     * Obtiene descripción de variante (Talla - Color).
     */
    public String getDescripcionVariante() {
        StringBuilder sb = new StringBuilder();
        if (nombreTalla != null) {
            sb.append(nombreTalla);
        }
        if (nombreTalla != null && nombreColor != null) {
            sb.append(" - ");
        }
        if (nombreColor != null) {
            sb.append(nombreColor);
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return String.format("%s x%d %s @ $%s = $%s",
                getDescripcionCompleta(),
                cantidad,
                tipoUnidad.getValor(),
                precioUnitario,
                subtotal);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        ModelCompraDetalle that = (ModelCompraDetalle) obj;

        if (idDetalleCompra != null && that.idDetalleCompra != null) {
            return idDetalleCompra.equals(that.idDetalleCompra);
        }

        return idVariante != null && idVariante.equals(that.idVariante)
                && idCompra != null && idCompra.equals(that.idCompra);
    }

    @Override
    public int hashCode() {
        if (idDetalleCompra != null) {
            return idDetalleCompra.hashCode();
        }
        int result = idVariante != null ? idVariante.hashCode() : 0;
        result = 31 * result + (idCompra != null ? idCompra.hashCode() : 0);
        return result;
    }
}
