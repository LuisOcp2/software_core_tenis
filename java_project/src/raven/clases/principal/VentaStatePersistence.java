package raven.clases.principal;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import raven.componentes.impresion.DataSearchClient;
import raven.controlador.principal.ModelMedioPago;

/**
 * Singleton class to persist the state of the sales form between transitions.
 */
public class VentaStatePersistence {
    private static VentaStatePersistence instance;

    private DataSearchClient selectClient;
    private List<Object[]> tableData;
    private List<ModelMedioPago> mediosPago;
    private String observaciones;
    private String estadoPago;
    private String tipoPago;
    private BigDecimal subTotal;
    private BigDecimal descuento;
    private BigDecimal total;
    private BigDecimal pendiente;
    private boolean hasState = false;

    private VentaStatePersistence() {
        tableData = new ArrayList<>();
        mediosPago = new ArrayList<>();
    }

    public static synchronized VentaStatePersistence getInstance() {
        if (instance == null) {
            instance = new VentaStatePersistence();
        }
        return instance;
    }

    public void clearState() {
        selectClient = null;
        tableData.clear();
        mediosPago.clear();
        observaciones = null;
        estadoPago = null;
        tipoPago = null;
        subTotal = BigDecimal.ZERO;
        descuento = BigDecimal.ZERO;
        total = BigDecimal.ZERO;
        pendiente = BigDecimal.ZERO;
        hasState = false;
    }

    // Getters and Setters
    public DataSearchClient getSelectClient() {
        return selectClient;
    }

    public void setSelectClient(DataSearchClient selectClient) {
        this.selectClient = selectClient;
        this.hasState = true;
    }

    public List<Object[]> getTableData() {
        return tableData;
    }

    public void setTableData(List<Object[]> tableData) {
        this.tableData = new ArrayList<>(tableData);
        this.hasState = true;
    }

    public List<ModelMedioPago> getMediosPago() {
        return mediosPago;
    }

    public void setMediosPago(List<ModelMedioPago> mediosPago) {
        this.mediosPago = new ArrayList<>(mediosPago);
        this.hasState = true;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
        this.hasState = true;
    }

    public String getEstadoPago() {
        return estadoPago;
    }

    public void setEstadoPago(String estadoPago) {
        this.estadoPago = estadoPago;
        this.hasState = true;
    }

    public String getTipoPago() {
        return tipoPago;
    }

    public void setTipoPago(String tipoPago) {
        this.tipoPago = tipoPago;
        this.hasState = true;
    }

    public BigDecimal getSubTotal() {
        return subTotal;
    }

    public void setSubTotal(BigDecimal subTotal) {
        this.subTotal = subTotal;
        this.hasState = true;
    }

    public BigDecimal getDescuento() {
        return descuento;
    }

    public void setDescuento(BigDecimal descuento) {
        this.descuento = descuento;
        this.hasState = true;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
        this.hasState = true;
    }

    public BigDecimal getPendiente() {
        return pendiente;
    }

    public void setPendiente(BigDecimal pendiente) {
        this.pendiente = pendiente;
        this.hasState = true;
    }

    public boolean hasState() {
        return hasState;
    }
}
