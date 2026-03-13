package raven.controlador.principal;

import org.apache.commons.dbcp2.BasicDataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import raven.controlador.segurity.Capa;

public class conexion_optimizada {
    private static conexion_optimizada instance;
    private BasicDataSource dataSource;
    
    // Configuración optimizada para alta latencia
    public static final String host = Capa.Luisa(Capa.host);
    public static final String port = "3306";
    public static final String database = Capa.Luisa(Capa.datsbase);
    public static final String username = Capa.Luisa(Capa.user);
    public static final String password = Capa.Luisa(Capa.password);

    public static conexion_optimizada getInstance() {
        if (instance == null) {
            instance = new conexion_optimizada();
        }
        return instance;
    }

    private conexion_optimizada() {
    }

    public synchronized void connectToDatabase() throws SQLException {
        if (dataSource == null) {
            initializeDataSource();
        }
        // Test connection
        try (Connection conn = createConnection()) {
            if (conn == null) {
                throw new SQLException("No se pudo establecer la conexión");
            }
        }
    }

    private void initializeDataSource() {
        try {
            dataSource = new BasicDataSource();

            // URL optimizada para alta latencia
            String url = "jdbc:mysql://" + host + ":" + port + "/" + database
                    + "?serverTimezone=America/Bogota"
                    + "&useSSL=false"
                    + "&allowPublicKeyRetrieval=true"
                    + "&useUnicode=true"
                    + "&characterEncoding=utf8"
                    // Optimizaciones para alta latencia
                    + "&connectTimeout=15000"      // 15 segundos para conectar
                    + "&socketTimeout=60000"       // 60 segundos máx. por consulta
                    + "&tcpKeepAlive=true"
                    // Pool de conexiones optimizado
                    + "&cachePrepStmts=true"
                    + "&prepStmtCacheSize=256"
                    + "&prepStmtCacheSqlLimit=2048"
                    + "&useServerPrepStmts=true"
                    + "&useLocalSessionState=true"
                    + "&rewriteBatchedStatements=true"
                    + "&useCompression=true"
                    + "&cacheResultSetMetadata=true"
                    + "&cacheServerConfiguration=true"
                    + "&elideSetAutoCommits=true"
                    + "&maintainTimeStats=false"
                    + "&useCursorFetch=true"
                    // Optimizaciones específicas para latencia
                    + "&interactiveClient=true"
                    + "&netTimeoutForStreamingResults=60000";

            dataSource.setUrl(url);
            dataSource.setUsername(username);
            dataSource.setPassword(password);
            dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");

            // Configuración del pool optimizado para alta latencia
            dataSource.setInitialSize(3);      // Conexiones iniciales
            dataSource.setMaxTotal(10);        // Máximo de conexiones (menos para evitar congestión)
            dataSource.setMaxIdle(5);          // Máximo inactivo
            dataSource.setMinIdle(2);          // Mínimo siempre disponible
            dataSource.setMaxWaitMillis(10000); // Espera máxima para obtener conexión

            // Configuración de limpieza para alta latencia
            dataSource.setTimeBetweenEvictionRunsMillis(30_000);       // Limpieza cada 30s
            dataSource.setMinEvictableIdleTimeMillis(120_000);         // 2 min antes de cerrar
            dataSource.setSoftMinEvictableIdleTimeMillis(60_000);      // 1 min para reducir si hay muchas
            dataSource.setNumTestsPerEvictionRun(2);

            dataSource.setRemoveAbandonedOnBorrow(true);
            dataSource.setRemoveAbandonedOnMaintenance(true);
            dataSource.setRemoveAbandonedTimeout(300); // 5 min sin usar -> abandonada
            dataSource.setLogAbandoned(true);

            // Configuración de prepared statements
            dataSource.setPoolPreparedStatements(true);
            dataSource.setMaxOpenPreparedStatements(50);

            // Configuración de transacciones
            dataSource.setDefaultAutoCommit(true);
            dataSource.setDefaultTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);

            // Configuración de validación (crítica para alta latencia)
            dataSource.setValidationQuery("SELECT 1");
            dataSource.setTestOnBorrow(false);  // Desactivado para evitar doble latencia
            dataSource.setTestOnReturn(false);
            dataSource.setTestWhileIdle(true);  // Solo validar mientras está inactivo
            dataSource.setValidationQueryTimeout(10); // Timeout más largo para alta latencia
            dataSource.setDefaultQueryTimeout(45); // Timeout de consulta aumentado

            // Tiempo de vida máximo de conexiones
            dataSource.setMaxConnLifetimeMillis(1200000); // 20 minutos

            // Configurar charset
            dataSource.setConnectionInitSqls(Arrays.asList(
                    "SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci",
                    "SET collation_connection = utf8mb4_unicode_ci"
            ));

            // Precalentar conexiones
            for (int i = 0; i < 2; i++) {
                try (Connection c = dataSource.getConnection()) {
                    // Devolver al pool
                }
            }

            try (Connection testConnection = dataSource.getConnection()) {
                if (testConnection != null && !testConnection.isClosed()) {
                    System.out.println("Conexión optimizada a base de datos establecida exitosamente");
                }
            }

        } catch (SQLException e) {
            System.err.println("Error al conectar a la base de datos optimizada:");
            System.err.println("Host: " + host + ":" + port);
            System.err.println("Database: " + database);
            System.err.println("Usuario: " + username);
            System.err.println("Error: " + e.getMessage());
        }
    }

    public Connection getConnection() throws SQLException {
        if (dataSource == null) {
            connectToDatabase();
        }
        return dataSource.getConnection();
    }

    public static Connection getConnectionStatic() throws SQLException {
        return getInstance().getConnection();
    }

    public Connection createConnection() throws SQLException {
        if (dataSource == null) {
            connectToDatabase();
        }
        return dataSource.getConnection();
    }

    public void shutdown() {
        try {
            if (dataSource != null) {
                dataSource.close();
                System.out.println("DataSource cerrado correctamente");
            }
        } catch (SQLException e) {
            System.err.println("Error al cerrar DataSource: " + e.getMessage());
        }
    }
}