package raven.controlador.principal;

import org.apache.commons.dbcp2.BasicDataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import raven.controlador.segurity.Capa;

public class conexion {
    private static conexion instance;
    private BasicDataSource dataSource;
    public static final String host = Capa.Luisa(Capa.host);
    public static final String port = "3306";
    //public static final String database = Capa.Luisa(Capa.datsbase);
    public static final String database = "siro_prueba"; //"siro_prueba"; //soft_xtreme
    public static final String username = Capa.Luisa(Capa.user);
    public static final String password = Capa.Luisa(Capa.password);
  // public static final String password = "";
    public static final String crisdev = "jdbc:mysql://" + host + "/" + database + username + password ;
    public static conexion getInstance() {
        if (instance == null) {
            // OPTIMIZACIÓN DNS: Cachear IPs para siempre (evita lookups repetidos a USA)
            java.security.Security.setProperty("networkaddress.cache.ttl", "-1");
            instance = new conexion();
        }
        return instance;
    }
    
    private conexion() {
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

        String url = "jdbc:mysql://" + host + ":" + port + "/" + database
                + "?serverTimezone=America/Bogota"
                + "&useSSL=false"
                + "&allowPublicKeyRetrieval=true"
                + "&useUnicode=true"
                + "&characterEncoding=utf8"
                // Optimizaciones para alta latencia (Colombia -> USA)
                + "&connectTimeout=" + AppConfig.DB_CONNECT_TIMEOUT
                + "&socketTimeout=" + AppConfig.DB_SOCKET_TIMEOUT
                + "&tcpKeepAlive=true"
                + "&cachePrepStmts=true"
                + "&prepStmtCacheSize=256"
                + "&prepStmtCacheSqlLimit=2048"
                + "&useServerPrepStmts=true"
                + "&useLocalSessionState=true"
                + "&rewriteBatchedStatements=true"
                + "&useCompression=true"       // Habilitar compresión para reducir tráfico
                + "&cacheResultSetMetadata=true"
                + "&cacheServerConfiguration=true"
                + "&elideSetAutoCommits=true"
                + "&maintainTimeStats=false"
                + "&useCursorFetch=true"
                + "&interactiveClient=true"    // Indicar que es una conexión interactiva
                + "&netTimeoutForStreamingResults=60000"; // Timeout para resultados de streaming

        dataSource.setUrl(url);
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");

        // ========= TAMAÑOS DEL POOL OPTIMIZADOS PARA ALTA LATENCIA =========
        dataSource.setInitialSize(AppConfig.DB_MIN_IDLE);
        dataSource.setMaxTotal(AppConfig.DB_MAX_TOTAL);
        dataSource.setMaxIdle(AppConfig.DB_MAX_IDLE);
        dataSource.setMinIdle(AppConfig.DB_MIN_IDLE);
        dataSource.setMaxWaitMillis(AppConfig.DB_MAX_WAIT_MILLIS);

        // ========= LIMPIEZA DE CONEXIONES AJUSTADA =========
        dataSource.setTimeBetweenEvictionRunsMillis(45_000);       // Limpieza cada 45s
        dataSource.setMinEvictableIdleTimeMillis(180_000);         // 3 min ociosas -> candidata a cerrar
        dataSource.setSoftMinEvictableIdleTimeMillis(120_000);     // 2 min para reducir si hay muchas
        dataSource.setNumTestsPerEvictionRun(2);

        dataSource.setRemoveAbandonedOnBorrow(true);
        dataSource.setRemoveAbandonedOnMaintenance(true);
        dataSource.setRemoveAbandonedTimeout(300); // 5 min sin cerrar -> se considera abandonada
        dataSource.setLogAbandoned(true);

        // ========= PREPARED STATEMENTS =========
        dataSource.setPoolPreparedStatements(true);
        dataSource.setMaxOpenPreparedStatements(75); // Reducido para alta latencia

        // ========= TRANSACCIONES / AUTO-COMMIT =========
        dataSource.setDefaultAutoCommit(true);
        dataSource.setDefaultTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);

        // ========= VALIDACIÓN AJUSTADA PARA ALTA LATENCIA =========
        dataSource.setValidationQuery("SELECT 1");
        // CRÍTICO PARA LATENCIA ALTA (Colombia -> USA):
        // Desactivamos testOnBorrow para que NO haga un ping (100ms+) cada vez que pedimos una conexión.
        // Confiamos en testWhileIdle y el tiempo de vida máximo.
        dataSource.setTestOnBorrow(false);
        dataSource.setTestOnReturn(false);
        dataSource.setTestWhileIdle(true);
        // Aumentar timeout de validación para evitar falsos negativos en latencia alta
        dataSource.setValidationQueryTimeout(10); // Aumentado para alta latencia
        dataSource.setDefaultQueryTimeout(45);    // Aumentado para consultas lentas

        // Tiempo de vida máximo de conexiones: 20 minutos (1,200,000ms)
        // Evita que conexiones viejas causen problemas, ajustado para alta latencia
        dataSource.setMaxConnLifetimeMillis(1200000);

        // Charset / collation correctos en cada conexión
        dataSource.setConnectionInitSqls(Arrays.asList(
                "SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci",
                "SET collation_connection = utf8mb4_unicode_ci"
        ));

        // Precalentar conexiones
        for (int i = 0; i < 2; i++) {
            try (Connection c = dataSource.getConnection()) {
                // se devuelve al pool
            }
        }

        try (Connection testConnection = dataSource.getConnection()) {
            if (testConnection != null && !testConnection.isClosed()) {
                System.out.println("Conexión optimizada a base de datos establecida exitosamente");
                System.out.println("Latencia estimada: Conexión a " + host + " desde Colombia");
            }
        }

    } catch (SQLException e) {
        System.err.println("Error al conectar a la base de datos:");
        System.err.println("Host: " + host + ":" + port);
        System.err.println("Database: " + database);
        System.err.println("Usuario: " + username);
        System.err.println("Error: " + e.getMessage());
    }
    }

    /**
     * Exponer el DataSource para repositorios que lo requieren
     */
    public javax.sql.DataSource getDataSource() {
        return this.dataSource;
    }

    public Connection getConnection() throws SQLException {
        if (dataSource == null) {
            connectToDatabase();
        }
        return dataSource.getConnection();
    }

    // Método estático para obtener conexión directamente
    public static Connection getConnectionStatic() throws SQLException {
        return getInstance().getConnection();
    }
    
    public void close(AutoCloseable... closeables) throws SQLException {
        try {
            for (AutoCloseable closeable : closeables) {
                if (closeable != null) {
                    closeable.close();
                }
            }
        } catch (Exception e) {
            throw new SQLException("Error al cerrar recursos: " + e.getMessage());
        }
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

    
    
    public Connection createConnection() throws SQLException {
        if (dataSource == null) {
            connectToDatabase();
        }
        return dataSource.getConnection();
    }
    
    public boolean isConnected() {
        return dataSource != null;
    }
}
