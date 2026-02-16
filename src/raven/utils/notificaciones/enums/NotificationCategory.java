package raven.utils.notificaciones.enums;

/**
 * Categorías de notificaciones para agrupamiento y filtrado.
 */
public enum NotificationCategory {
    TRASPASOS("traspasos"),
    VENTAS("ventas"),
    INVENTARIO("inventario"),
    SISTEMA("sistema"),
    ORDEN_VENTA("ordenes_reserva");

    private final String dbValue;

    NotificationCategory(String dbValue) {
        this.dbValue = dbValue;
    }

    public String getDbValue() {
        return dbValue;
    }
}
