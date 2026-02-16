package raven.controlador.principal;


/**
 * Excepción base para errores de negocio del módulo de gastos y compras.
 * 
 * Principio aplicado: Fail Fast - Errores claros y específicos
 * 
 * @author CrisDEV
 * @version 1.0
 */
public class GastosException extends RuntimeException {
    
    private final String codigoError;
    private final Object[] detalles;
    
    public GastosException(String mensaje) {
        super(mensaje);
        this.codigoError = "GAS-000";
        this.detalles = new Object[0];
    }
    
    public GastosException(String codigoError, String mensaje) {
        super(mensaje);
        this.codigoError = codigoError;
        this.detalles = new Object[0];
    }
    
    public GastosException(String codigoError, String mensaje, Object... detalles) {
        super(mensaje);
        this.codigoError = codigoError;
        this.detalles = detalles;
    }
    
    public GastosException(String mensaje, Throwable causa) {
        super(mensaje, causa);
        this.codigoError = "GAS-000";
        this.detalles = new Object[0];
    }
    
    public String getCodigoError() {
        return codigoError;
    }
    
    public Object[] getDetalles() {
        return detalles;
    }
    
    @Override
    public String toString() {
        return String.format("[%s] %s", codigoError, getMessage());
    }
}

/**
 * Excepción cuando no hay caja abierta.
 */
class CajaNoActivaException extends GastosException {
    
    public CajaNoActivaException() {
        super("GAS-001", "No hay caja activa. Abra la caja antes de registrar gastos.");
    }
    
    public CajaNoActivaException(String mensaje) {
        super("GAS-001", mensaje);
    }
}

/**
 * Excepción cuando no hay sesión activa.
 */
class SesionNoActivaException extends GastosException {
    
    public SesionNoActivaException() {
        super("GAS-002", "No hay sesión activa. Inicie sesión nuevamente.");
    }
}

/**
 * Excepción para validaciones de datos.
 */
class ValidacionException extends GastosException {
    
    public ValidacionException(String campo, String mensaje) {
        super("GAS-003", String.format("Error en campo '%s': %s", campo, mensaje), campo);
    }
    
    public ValidacionException(String mensaje) {
        super("GAS-003", mensaje);
    }
}

/**
 * Excepción cuando un gasto requiere autorización.
 */
class AutorizacionRequeridaException extends GastosException {
    
    private final java.math.BigDecimal monto;
    private final java.math.BigDecimal montoMaximo;
    
    public AutorizacionRequeridaException(java.math.BigDecimal monto, java.math.BigDecimal montoMaximo) {
        super("GAS-004", 
            String.format("El monto $%s requiere autorización. Máximo sin autorización: $%s", 
                monto, montoMaximo),
            monto, montoMaximo);
        this.monto = monto;
        this.montoMaximo = montoMaximo;
    }
    
    public java.math.BigDecimal getMonto() {
        return monto;
    }
    
    public java.math.BigDecimal getMontoMaximo() {
        return montoMaximo;
    }
}

/**
 * Excepción cuando un gasto no puede ser anulado.
 */
class GastoNoAnulableException extends GastosException {
    
    public GastoNoAnulableException(int idGasto, String estadoActual) {
        super("GAS-005", 
            String.format("El gasto #%d no puede anularse. Estado actual: %s", 
                idGasto, estadoActual),
            idGasto, estadoActual);
    }
}

/**
 * Excepción para errores de inventario.
 */
class InventarioException extends GastosException {
    
    public InventarioException(String mensaje) {
        super("GAS-006", mensaje);
    }
    
    public InventarioException(int idVariante, String mensaje) {
        super("GAS-006", String.format("Error en variante #%d: %s", idVariante, mensaje), idVariante);
    }
}