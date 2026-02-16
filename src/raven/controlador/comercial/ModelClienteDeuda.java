package raven.controlador.comercial;

import java.math.BigDecimal;

public class ModelClienteDeuda extends ModelCliente {

    private BigDecimal deudaTotal;
    private int cantidadVentasPendientes;

    public ModelClienteDeuda() {
        super();
        this.deudaTotal = BigDecimal.ZERO;
        this.cantidadVentasPendientes = 0;
    }

    public BigDecimal getDeudaTotal() {
        return deudaTotal;
    }

    public void setDeudaTotal(BigDecimal deudaTotal) {
        this.deudaTotal = deudaTotal;
    }

    public int getCantidadVentasPendientes() {
        return cantidadVentasPendientes;
    }

    public void setCantidadVentasPendientes(int cantidadVentasPendientes) {
        this.cantidadVentasPendientes = cantidadVentasPendientes;
    }

    public Object[] toRowCuentasPorCobrar(int rowNum) {
        return new Object[] {
                false, // Checkbox
                rowNum,
                this, // Object (hidden or used for selection)
                getDni(),
                getNombre(),
                getTelefono(),
                cantidadVentasPendientes,
                raven.clases.principal.MoneyFormatter.format(deudaTotal.doubleValue())
        };
    }
}
