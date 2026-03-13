// ====================================================================
// ModelDetalleDevolucion.java CORREGIDO
// ====================================================================
package raven.controlador.comercial;

import java.math.BigDecimal;

public class ModelDetalleDevolucion {

    private int idDetalleDevolucion;
    private int idDevolucion;
    private int idDetalleVenta;
    private int idProducto;
    private int idVariante; // Puede ser 0 para productos sin variantes
    private int cantidadDevuelta;
    private int cantidadOriginal;
    private BigDecimal precioUnitarioOriginal;
    private BigDecimal descuentoOriginal;
    private BigDecimal subtotalDevolucion;
    private MotivoDetalle motivoDetalle;
    private CondicionProducto condicionProducto;
    private AccionProducto accionProducto;
    private String observacionesDetalle;
    private boolean activo;

    // Campos temporales para UI (No están en tabla devolucion_detalles)
    private String nombreProducto;
    private String nombreVariante;

    // Constructor
    public ModelDetalleDevolucion() {
        this.descuentoOriginal = BigDecimal.ZERO;
        this.subtotalDevolucion = BigDecimal.ZERO;
        this.condicionProducto = CondicionProducto.NUEVO;
        this.accionProducto = AccionProducto.REINGRESO_INVENTARIO;
        this.activo = true;
    }

    // Getters y Setters
    public int getIdDetalleDevolucion() {
        return idDetalleDevolucion;
    }

    public void setIdDetalleDevolucion(int idDetalleDevolucion) {
        this.idDetalleDevolucion = idDetalleDevolucion;
    }

    public int getIdDevolucion() {
        return idDevolucion;
    }

    public void setIdDevolucion(int idDevolucion) {
        this.idDevolucion = idDevolucion;
    }

    public int getIdDetalleVenta() {
        return idDetalleVenta;
    }

    public void setIdDetalleVenta(int idDetalleVenta) {
        this.idDetalleVenta = idDetalleVenta;
    }

    public int getIdProducto() {
        return idProducto;
    }

    public void setIdProducto(int idProducto) {
        this.idProducto = idProducto;
    }

    public int getIdVariante() {
        return idVariante;
    }

    public void setIdVariante(int idVariante) {
        this.idVariante = idVariante;
    }

    public int getCantidadDevuelta() {
        return cantidadDevuelta;
    }

    public void setCantidadDevuelta(int cantidadDevuelta) {
        this.cantidadDevuelta = cantidadDevuelta;
    }

    public int getCantidadOriginal() {
        return cantidadOriginal;
    }

    public void setCantidadOriginal(int cantidadOriginal) {
        this.cantidadOriginal = cantidadOriginal;
    }

    public BigDecimal getPrecioUnitarioOriginal() {
        return precioUnitarioOriginal;
    }

    public void setPrecioUnitarioOriginal(BigDecimal precioUnitarioOriginal) {
        this.precioUnitarioOriginal = precioUnitarioOriginal;
    }

    public BigDecimal getDescuentoOriginal() {
        return descuentoOriginal;
    }

    public void setDescuentoOriginal(BigDecimal descuentoOriginal) {
        this.descuentoOriginal = descuentoOriginal;
    }

    public BigDecimal getSubtotalDevolucion() {
        return subtotalDevolucion;
    }

    public void setSubtotalDevolucion(BigDecimal subtotalDevolucion) {
        this.subtotalDevolucion = subtotalDevolucion;
    }

    public MotivoDetalle getMotivoDetalle() {
        return motivoDetalle;
    }

    public void setMotivoDetalle(MotivoDetalle motivoDetalle) {
        this.motivoDetalle = motivoDetalle;
    }

    public CondicionProducto getCondicionProducto() {
        return condicionProducto;
    }

    public void setCondicionProducto(CondicionProducto condicionProducto) {
        this.condicionProducto = condicionProducto;
    }

    public AccionProducto getAccionProducto() {
        return accionProducto;
    }

    public void setAccionProducto(AccionProducto accionProducto) {
        this.accionProducto = accionProducto;
    }

    public String getObservacionesDetalle() {
        return observacionesDetalle;
    }

    public void setObservacionesDetalle(String observacionesDetalle) {
        this.observacionesDetalle = observacionesDetalle;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    public String getNombreProducto() {
        return nombreProducto;
    }

    public void setNombreProducto(String nombreProducto) {
        this.nombreProducto = nombreProducto;
    }

    public String getNombreVariante() {
        return nombreVariante;
    }

    public void setNombreVariante(String nombreVariante) {
        this.nombreVariante = nombreVariante;
    }

    /**
     * Determina si el producto puede reintegrarse al inventario
     */
    public boolean puedeReintegrarseInventario() {
        return condicionProducto == CondicionProducto.NUEVO ||
                condicionProducto == CondicionProducto.USADO_BUENO;
    }

    // Enums
    public enum MotivoDetalle {
        DEFECTO_FABRICA("defecto_fabrica", "Defecto de Fábrica"),
        TALLA_INCORRECTA("talla_incorrecta", "Talla Incorrecta"),
        PRODUCTO_DANADO("producto_dañado", "Producto Dañado"),
        INSATISFACCION("insatisfaccion", "Insatisfacción"),
        OTROS("otros", "Otros");

        private final String valor;
        private final String descripcion;

        MotivoDetalle(String valor, String descripcion) {
            this.valor = valor;
            this.descripcion = descripcion;
        }

        public String getValor() {
            return valor;
        }

        public String getDescripcion() {
            return descripcion;
        }

        public static MotivoDetalle fromString(String valor) {
            for (MotivoDetalle motivo : MotivoDetalle.values()) {
                if (motivo.valor.equals(valor)) {
                    return motivo;
                }
            }
            return OTROS;
        }
    }

    public enum CondicionProducto {
        NUEVO("nuevo", "Nuevo"),
        USADO_BUENO("usado_bueno", "Usado - Buen Estado"),
        USADO_REGULAR("usado_regular", "Usado - Estado Regular"),
        DANADO("dañado", "Dañado"),
        DEFECTUOSO("defectuoso", "Defectuoso");

        private final String valor;
        private final String descripcion;

        CondicionProducto(String valor, String descripcion) {
            this.valor = valor;
            this.descripcion = descripcion;
        }

        public String getValor() {
            return valor;
        }

        public String getDescripcion() {
            return descripcion;
        }

        public static CondicionProducto fromString(String valor) {
            for (CondicionProducto condicion : CondicionProducto.values()) {
                if (condicion.valor.equals(valor)) {
                    return condicion;
                }
            }
            return NUEVO;
        }
    }

    public enum AccionProducto {
        REINGRESO_INVENTARIO("reingreso_inventario", "Reingreso a Inventario"),
        REPARACION("reparacion", "Enviar a Reparación"),
        DESCARTE("descarte", "Descartar"),
        DEVOLUCION_PROVEEDOR("devolucion_proveedor", "Devolver a Proveedor");

        private final String valor;
        private final String descripcion;

        AccionProducto(String valor, String descripcion) {
            this.valor = valor;
            this.descripcion = descripcion;
        }

        public String getValor() {
            return valor;
        }

        public String getDescripcion() {
            return descripcion;
        }

        public static AccionProducto fromString(String valor) {
            for (AccionProducto accion : AccionProducto.values()) {
                if (accion.valor.equals(valor)) {
                    return accion;
                }
            }
            return REINGRESO_INVENTARIO;
        }
    }
}