package raven.controlador.admin;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Modelo que representa el resumen completo de cierre de caja.
 * 
 * Contiene información detallada sobre ventas, pagos recibidos
 * y desglose por método de pago para un movimiento de caja específico.
 * 
 * @author Sistema
 * @version 1.0
 */
public class ResumenCierreCaja {

    // ==================== ATRIBUTOS ====================
    private int idMovimiento;
    private int totalVentas; // Número total de ventas
    private int totalPagosRecibidos; // Número total de pagos recibidos
    private BigDecimal montoTotalVentas; // Suma total de ventas

    // Gastos operativos
    private int totalGastos; // Número total de gastos
    private BigDecimal montoTotalGastos; // Suma total de gastos

    // Compras externas
    private int totalCompras; // Número total de compras externas
    private BigDecimal montoTotalCompras; // Suma total de compras externas

    // Desglose por tipo de pago (ordenado por monto)
    private Map<String, DetallePago> detallesPorTipo;

    // Desglose por productos vendidos
    private List<DetalleProducto> productosVendidos;

    // ==================== CONSTRUCTOR ====================
    public ResumenCierreCaja() {
        this.totalVentas = 0;
        this.totalPagosRecibidos = 0;
        this.montoTotalVentas = BigDecimal.ZERO;
        this.totalGastos = 0;
        this.montoTotalGastos = BigDecimal.ZERO;
        this.totalCompras = 0;
        this.montoTotalCompras = BigDecimal.ZERO;
        this.detallesPorTipo = new LinkedHashMap<>();
        this.productosVendidos = new ArrayList<>();
    }

    public ResumenCierreCaja(int idMovimiento) {
        this();
        this.idMovimiento = idMovimiento;
    }

    // ==================== CLASES INTERNAS ====================
    /**
     * Representa el resumen de productos vendidos.
     */
    public static class DetalleProducto {
        private String nombreProducto;
        private int cantidad;
        private String unidad; // "pares", "cajas", etc.
        private BigDecimal montoTotal;

        public DetalleProducto(String nombre, int cantidad, String unidad, BigDecimal monto) {
            this.nombreProducto = nombre;
            this.cantidad = cantidad;
            this.unidad = unidad;
            this.montoTotal = monto;
        }

        public String getNombreProducto() {
            return nombreProducto;
        }

        public int getCantidad() {
            return cantidad;
        }

        public String getUnidad() {
            return unidad;
        }

        public BigDecimal getMontoTotal() {
            return montoTotal;
        }

        @Override
        public String toString() {
            return String.format("%d %s %s -> $%,.2f",
                    cantidad, unidad, nombreProducto, montoTotal);
        }
    }

    /**
     * Representa el detalle de pagos por tipo específico.
     */
    public static class DetallePago {
        private String tipoPago; // efectivo, tarjeta, transferencia, etc.
        private String descripcion; // Nombre legible (Efectivo, Tarjeta de Crédito, etc.)
        private int cantidadPagos; // Número de pagos de este tipo
        private BigDecimal montoTotal; // Suma total de este tipo

        public DetallePago() {
            this.cantidadPagos = 0;
            this.montoTotal = BigDecimal.ZERO;
        }

        public DetallePago(String tipoPago, String descripcion) {
            this();
            this.tipoPago = tipoPago;
            this.descripcion = descripcion;
        }

        // Getters y Setters
        public String getTipoPago() {
            return tipoPago;
        }

        public void setTipoPago(String tipoPago) {
            this.tipoPago = tipoPago;
        }

        public String getDescripcion() {
            return descripcion;
        }

        public void setDescripcion(String descripcion) {
            this.descripcion = descripcion;
        }

        public int getCantidadPagos() {
            return cantidadPagos;
        }

        public void setCantidadPagos(int cantidadPagos) {
            this.cantidadPagos = cantidadPagos;
        }

        public BigDecimal getMontoTotal() {
            return montoTotal;
        }

        public void setMontoTotal(BigDecimal montoTotal) {
            this.montoTotal = montoTotal;
        }

        /**
         * Agrega un pago al detalle.
         */
        public void agregarPago(BigDecimal monto) {
            this.cantidadPagos++;
            this.montoTotal = this.montoTotal.add(monto);
        }

        @Override
        public String toString() {
            return String.format("%s: %d pagos - $%,.2f",
                    descripcion, cantidadPagos, montoTotal);
        }
    }

    // ==================== MÉTODOS ====================

    /**
     * Agrega o actualiza un tipo de pago en el desglose.
     */
    public void agregarDetallePago(String tipoPago, int cantidad, BigDecimal monto) {
        DetallePago detalle = detallesPorTipo.getOrDefault(tipoPago,
                new DetallePago(tipoPago, obtenerDescripcionTipo(tipoPago)));

        detalle.setCantidadPagos(detalle.getCantidadPagos() + cantidad);
        detalle.setMontoTotal(detalle.getMontoTotal().add(monto));

        detallesPorTipo.put(tipoPago, detalle);
    }

    /**
     * Obtiene descripción legible para el tipo de pago.
     */
    public static String obtenerDescripcionTipo(String tipoPago) {
        if (tipoPago == null)
            return "Otro";

        switch (tipoPago.toLowerCase()) {
            case "efectivo":
                return "Efectivo";
            case "tarjeta":
                return "Tarjeta";
            case "tarjeta_credito":
                return "Tarjeta Crédito";
            case "tarjeta_debito":
                return "Tarjeta Débito";
            case "transferencia":
                return "Transferencia";
            case "sistecredito":
                return "Sistecredito";
            case "nota_credito":
                return "Nota Crédito";
            case "mixto":
                return "Pago Mixto";
            case "otro":
                return "Otro";
            default:
                return tipoPago;
        }
    }

    /**
     * Obtiene emoji para el tipo de pago.
     */
    public static String obtenerEmojiTipo(String tipoPago) {
        if (tipoPago == null)
            return "OT";

        switch (tipoPago.toLowerCase()) {
            case "efectivo":
                return "EF";
            case "tarjeta":
            case "tarjeta_credito":
            case "tarjeta_debito":
                return "TJ";
            case "transferencia":
                return "TR";
            case "sistecredito":
                return "SC";
            case "nota_credito":
                return "NC";
            case "mixto":
                return "MX";
            default:
                return "OT";
        }
    }

    // ==================== GETTERS Y SETTERS ====================

    public int getIdMovimiento() {
        return idMovimiento;
    }

    public void setIdMovimiento(int idMovimiento) {
        this.idMovimiento = idMovimiento;
    }

    public int getTotalVentas() {
        return totalVentas;
    }

    public void setTotalVentas(int totalVentas) {
        this.totalVentas = totalVentas;
    }

    public int getTotalPagosRecibidos() {
        return totalPagosRecibidos;
    }

    public void setTotalPagosRecibidos(int totalPagosRecibidos) {
        this.totalPagosRecibidos = totalPagosRecibidos;
    }

    public BigDecimal getMontoTotalVentas() {
        return montoTotalVentas;
    }

    public void setMontoTotalVentas(BigDecimal montoTotalVentas) {
        this.montoTotalVentas = montoTotalVentas;
    }

    public Map<String, DetallePago> getDetallesPorTipo() {
        return detallesPorTipo;
    }

    public void setDetallesPorTipo(Map<String, DetallePago> detallesPorTipo) {
        this.detallesPorTipo = detallesPorTipo;
    }

    public List<DetalleProducto> getProductosVendidos() {
        return productosVendidos;
    }

    public void setProductosVendidos(List<DetalleProducto> productosVendidos) {
        this.productosVendidos = productosVendidos;
    }

    public void agregarProductoVendido(DetalleProducto detalle) {
        this.productosVendidos.add(detalle);
    }

    // ==================== GETTERS/SETTERS GASTOS ====================
    public int getTotalGastos() {
        return totalGastos;
    }

    public void setTotalGastos(int totalGastos) {
        this.totalGastos = totalGastos;
    }

    public BigDecimal getMontoTotalGastos() {
        return montoTotalGastos;
    }

    public void setMontoTotalGastos(BigDecimal montoTotalGastos) {
        this.montoTotalGastos = montoTotalGastos;
    }

    // ==================== GETTERS/SETTERS COMPRAS ====================
    public int getTotalCompras() {
        return totalCompras;
    }

    public void setTotalCompras(int totalCompras) {
        this.totalCompras = totalCompras;
    }

    public BigDecimal getMontoTotalCompras() {
        return montoTotalCompras;
    }

    public void setMontoTotalCompras(BigDecimal montoTotalCompras) {
        this.montoTotalCompras = montoTotalCompras;
    }

    /**
     * Calcula el total de pagos recibidos sumando todos los detalles.
     */
    public void calcularTotalPagosRecibidos() {
        int total = 0;
        for (DetallePago detalle : detallesPorTipo.values()) {
            total += detalle.getCantidadPagos();
        }
        this.totalPagosRecibidos = total;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ResumenCierreCaja {\n");
        sb.append(String.format("  Movimiento: #%d\n", idMovimiento));
        sb.append(String.format("  Total Ventas: %d\n", totalVentas));
        sb.append(String.format("  Total Pagos: %d\n", totalPagosRecibidos));
        sb.append(String.format("  Monto Ventas: $%,.2f\n", montoTotalVentas));
        sb.append(String.format("  Total Gastos: %d - $%,.2f\n", totalGastos, montoTotalGastos));
        sb.append(String.format("  Total Compras: %d - $%,.2f\n", totalCompras, montoTotalCompras));
        sb.append("  Desglose Pagos:\n");
        for (DetallePago detalle : detallesPorTipo.values()) {
            sb.append(String.format("    - %s\n", detalle));
        }
        sb.append("}");
        return sb.toString();
    }
}
