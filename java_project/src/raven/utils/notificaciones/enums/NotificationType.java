package raven.utils.notificaciones.enums;

/**
 * Tipos de notificaciones soportados por el sistema.
 * Mapea los valores almacenados en la base de datos.
 */
public enum NotificationType {
    URGENT("urgent"),
    ERROR("error"),
    WARNING("warning"),
    INFO("info"),
    SUCCESS("success");

    private final String dbValue;

    NotificationType(String dbValue) {
        this.dbValue = dbValue;
    }

    public String getDbValue() {
        return dbValue;
    }

    public static NotificationType fromString(String value) {
        for (NotificationType type : values()) {
            if (type.dbValue.equalsIgnoreCase(value)) {
                return type;
            }
        }
        return INFO; // Default
    }
}
