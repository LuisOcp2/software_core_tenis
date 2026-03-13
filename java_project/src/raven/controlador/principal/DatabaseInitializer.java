package raven.controlador.principal;

import java.sql.SQLException;

public class DatabaseInitializer {
    private static boolean initialized = false;
    
    public static synchronized void initializeDatabase() throws SQLException {
        if (!initialized) {
            conexion.getInstance().connectToDatabase();
            
            // Ejecutar actualizaciones de base de datos
            try {
                raven.utils.DBUpdater.runUpdates();
            } catch (Exception e) {
                System.err.println("Error ejecutando actualizaciones de DB: " + e.getMessage());
                e.printStackTrace();
            }
            
            initialized = true;
        }
    }
    
    public static boolean isInitialized() {
        return initialized;
    }
}
