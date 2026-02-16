package raven.utils.notificaciones.enums;

/**
 * Define los niveles de prioridad para las notificaciones.
 * Menor valor numérico indica mayor prioridad.
 */
public enum NotificationPriority {
    URGENT(0),
    HIGH(1),
    MEDIUM(2),
    LOW(3);

    private final int value;

    NotificationPriority(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static NotificationPriority fromString(String type) {
        if (type == null) return LOW;
        switch (type.toLowerCase()) {
            case "urgent":
            case "error":
                return URGENT;
            case "warning":
            case "alert":
                return HIGH;
            case "info":
            case "success":
                return MEDIUM;
            default:
                return LOW;
        }
    }
}
