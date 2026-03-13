package raven.clases.inventario;

import raven.controlador.inventario.InventarioBodega;
import raven.controlador.principal.conexion;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.math.BigDecimal;

/**
 * Servicio para gestionar inventario multi-bodega.
 * 
 * Esta clase implementa el patrón Repository y Service Layer para encapsular
 * toda la lógica de negocio relacionada con el inventario distribuido en múltiples bodegas.
 * 
 * Principios SOLID aplicados:
 * - Single Responsibility: Gestiona únicamente el inventario de bodegas
 * - Open/Closed: Extensible mediante herencia, cerrado a modificación
 * - Liskov Substitution: Puede ser sustituido por subclases
 * - Interface Segregation: Métodos cohesivos y específicos
 * - Dependency Inversion: Depende de abstracciones (Connection) no de implementaciones
 * 
 * Patrones de Diseño:
 * - Repository Pattern: Abstrae el acceso a datos
 * - DAO Pattern: Separa lógica de persistencia
 * - Builder Pattern: Para construir objetos complejos
 * 
 * @author Sistema de Gestión Multi-Bodega
 * @version 1.0
 */
public class ServiceInventarioBodega {
    
    // ==================== CONSTANTES SQL ====================
    
    /**
     * Consulta para obtener inventario de una variante en todas las bodegas.
     * Incluye información completa del producto, variante y bodega.
     */
    private static final String SQL_GET_INVENTARIO_BY_VARIANTE =
        "SELECT " +
        "    ib.id_inventario_bodega, " +
        "    ib.id_bodega, " +
        "    b.nombre AS nombre_bodega, " +
        "    b.codigo AS codigo_bodega, " +
        "    ib.id_variante, " +
        "    pv.sku, " +
        "    pv.id_producto, " +
        "    p.nombre AS nombre_producto, " +
        "    p.codigo_modelo, " +
        "    t.numero AS talla, " +
        "    c.nombre AS color, " +
        "    ib.Stock_par, " +
        "    ib.Stock_caja, " +
        "    ib.stock_reservado, " +
        "    ib.ubicacion_especifica, " +
        "    ib.fecha_ultimo_movimiento, " +
        "    ib.fecha_actualizacion, " +
        "    ib.activo, " +
        "    pv.precio_compra, " +
        "    pv.precio_venta, " +
        "    pv.stock_minimo_variante, " +
        "    p.pares_por_caja " +
        "FROM inventario_bodega ib " +
        "INNER JOIN bodegas b ON ib.id_bodega = b.id_bodega " +
        "INNER JOIN producto_variantes pv ON ib.id_variante = pv.id_variante " +
        "INNER JOIN productos p ON pv.id_producto = p.id_producto " +
        "INNER JOIN tallas t ON pv.id_talla = t.id_talla " +
        "INNER JOIN colores c ON pv.id_color = c.id_color " +
        "WHERE ib.id_variante = ? " +
        "AND ib.activo = 1 " +
        "ORDER BY b.nombre";
    
    /**
     * Consulta para obtener inventario de un producto en una bodega específica.
     * Trae todas las variantes del producto en esa bodega.
     */
    private static final String SQL_GET_INVENTARIO_BY_PRODUCTO_BODEGA =
        "SELECT " +
        "    ib.id_inventario_bodega, " +
        "    ib.id_bodega, " +
        "    b.nombre AS nombre_bodega, " +
        "    b.codigo AS codigo_bodega, " +
        "    ib.id_variante, " +
        "    pv.sku, " +
        "    pv.id_producto, " +
        "    p.nombre AS nombre_producto, " +
        "    p.codigo_modelo, " +
        "    t.numero AS talla, " +
        "    c.nombre AS color, " +
        "    ib.Stock_par, " +
        "    ib.Stock_caja, " +
        "    ib.stock_reservado, " +
        "    ib.ubicacion_especifica, " +
        "    ib.fecha_ultimo_movimiento, " +
        "    ib.fecha_actualizacion, " +
        "    ib.activo, " +
        "    pv.precio_compra, " +
        "    pv.precio_venta, " +
        "    pv.stock_minimo_variante, " +
        "    p.pares_por_caja " +
        "FROM inventario_bodega ib " +
        "INNER JOIN bodegas b ON ib.id_bodega = b.id_bodega " +
        "INNER JOIN producto_variantes pv ON ib.id_variante = pv.id_variante " +
        "INNER JOIN productos p ON pv.id_producto = p.id_producto " +
        "INNER JOIN tallas t ON pv.id_talla = t.id_talla " +
        "INNER JOIN colores c ON pv.id_color = c.id_color " +
        "WHERE p.id_producto = ? " +
        "AND ib.id_bodega = ? " +
        "AND ib.activo = 1 " +
        "ORDER BY t.numero, c.nombre";
    
    /**
     * Consulta para obtener todo el inventario de una bodega.
     */
    private static final String SQL_GET_INVENTARIO_BY_BODEGA =
        "SELECT " +
        "    ib.id_inventario_bodega, " +
        "    ib.id_bodega, " +
        "    b.nombre AS nombre_bodega, " +
        "    b.codigo AS codigo_bodega, " +
        "    ib.id_variante, " +
        "    pv.sku, " +
        "    pv.id_producto, " +
        "    p.nombre AS nombre_producto, " +
        "    p.codigo_modelo, " +
        "    t.numero AS talla, " +
        "    c.nombre AS color, " +
        "    ib.Stock_par, " +
        "    ib.Stock_caja, " +
        "    ib.stock_reservado, " +
        "    ib.ubicacion_especifica, " +
        "    ib.fecha_ultimo_movimiento, " +
        "    ib.fecha_actualizacion, " +
        "    ib.activo, " +
        "    pv.precio_compra, " +
        "    pv.precio_venta, " +
        "    pv.stock_minimo_variante, " +
        "    p.pares_por_caja " +
        "FROM inventario_bodega ib " +
        "INNER JOIN bodegas b ON ib.id_bodega = b.id_bodega " +
        "INNER JOIN producto_variantes pv ON ib.id_variante = pv.id_variante " +
        "INNER JOIN productos p ON pv.id_producto = p.id_producto " +
        "INNER JOIN tallas t ON pv.id_talla = t.id_talla " +
        "INNER JOIN colores c ON pv.id_color = c.id_color " +
        "WHERE ib.id_bodega = ? " +
        "AND ib.activo = 1 " +
        "ORDER BY p.nombre, t.numero, c.nombre";
    
    /**
     * Consulta para verificar si existe inventario de una variante en una bodega.
     */
    private static final String SQL_CHECK_INVENTARIO_EXISTS =
        "SELECT id_inventario_bodega, Stock_par, Stock_caja, stock_reservado " +
        "FROM inventario_bodega " +
        "WHERE id_bodega = ? AND id_variante = ? AND activo = 1";
    
    /**
     * Consulta para insertar nuevo registro de inventario.
     */
    private static final String SQL_INSERT_INVENTARIO =
        "INSERT INTO inventario_bodega " +
        "(id_bodega, id_variante, Stock_par, Stock_caja, stock_reservado, " +
        "ubicacion_especifica, fecha_ultimo_movimiento, activo) " +
        "VALUES (?, ?, ?, ?, ?, ?, NOW(), 1)";
    
    /**
     * Consulta para actualizar stock de inventario.
     */
    private static final String SQL_UPDATE_STOCK =
        "UPDATE inventario_bodega " +
        "SET Stock_par = ?, Stock_caja = ?, stock_reservado = ?, " +
        "fecha_ultimo_movimiento = NOW() " +
        "WHERE id_inventario_bodega = ?";
    
    /**
     * Consulta para actualizar ubicación específica.
     */
    private static final String SQL_UPDATE_UBICACION =
        "UPDATE inventario_bodega " +
        "SET ubicacion_especifica = ? " +
        "WHERE id_inventario_bodega = ?";
    
    /**
     * Consulta para obtener stock consolidado de una variante (suma de todas las bodegas).
     */
    private static final String SQL_GET_STOCK_CONSOLIDADO =
        "SELECT " +
        "    SUM(Stock_par) AS total_pares, " +
        "    SUM(Stock_caja) AS total_cajas, " +
        "    SUM(stock_reservado) AS total_reservado " +
        "FROM inventario_bodega " +
        "WHERE id_variante = ? AND activo = 1";
    
    /**
     * Consulta para obtener productos con stock bajo en una bodega.
     */
    private static final String SQL_GET_PRODUCTOS_STOCK_BAJO =
        "SELECT " +
        "    ib.id_inventario_bodega, " +
        "    ib.id_bodega, " +
        "    b.nombre AS nombre_bodega, " +
        "    ib.id_variante, " +
        "    pv.sku, " +
        "    p.nombre AS nombre_producto, " +
        "    t.numero AS talla, " +
        "    c.nombre AS color, " +
        "    ib.Stock_par, " +
        "    pv.stock_minimo_variante " +
        "FROM inventario_bodega ib " +
        "INNER JOIN bodegas b ON ib.id_bodega = b.id_bodega " +
        "INNER JOIN producto_variantes pv ON ib.id_variante = pv.id_variante " +
        "INNER JOIN productos p ON pv.id_producto = p.id_producto " +
        "INNER JOIN tallas t ON pv.id_talla = t.id_talla " +
        "INNER JOIN colores c ON pv.id_color = c.id_color " +
        "WHERE ib.id_bodega = ? " +
        "AND ib.Stock_par < pv.stock_minimo_variante " +
        "AND ib.activo = 1 " +
        "ORDER BY p.nombre";
    
    // ==================== MÉTODOS DE CONSULTA ====================
    
    /**
     * Obtiene el inventario de una variante en todas las bodegas.
     * 
     * @param idVariante ID de la variante a consultar
     * @return Lista de registros de inventario en diferentes bodegas
     * @throws SQLException Si hay error en la consulta
     */
    public List<InventarioBodega> getInventarioByVariante(int idVariante) throws SQLException {
        List<InventarioBodega> inventarios = new ArrayList<>();
        
        try (Connection con = conexion.getInstance().createConnection();
             PreparedStatement pst = con.prepareStatement(SQL_GET_INVENTARIO_BY_VARIANTE)) {
            
            pst.setInt(1, idVariante);
            
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    inventarios.add(mapResultSetToInventario(rs));
                }
            }
        }
        
        return inventarios;
    }
    
    /**
     * Obtiene el inventario de un producto específico en una bodega.
     * Retorna todas las variantes del producto en esa bodega.
     * 
     * @param idProducto ID del producto
     * @param idBodega ID de la bodega
     * @return Lista de inventarios de las variantes del producto
     * @throws SQLException Si hay error en la consulta
     */
    public List<InventarioBodega> getInventarioByProductoBodega(int idProducto, int idBodega) 
            throws SQLException {
        List<InventarioBodega> inventarios = new ArrayList<>();
        
        try (Connection con = conexion.getInstance().createConnection();
             PreparedStatement pst = con.prepareStatement(SQL_GET_INVENTARIO_BY_PRODUCTO_BODEGA)) {
            
            pst.setInt(1, idProducto);
            pst.setInt(2, idBodega);
            
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    inventarios.add(mapResultSetToInventario(rs));
                }
            }
        }
        
        return inventarios;
    }
    
    /**
     * Obtiene todo el inventario de una bodega específica.
     * 
     * @param idBodega ID de la bodega
     * @return Lista completa de inventarios en la bodega
     * @throws SQLException Si hay error en la consulta
     */
    public List<InventarioBodega> getInventarioByBodega(int idBodega) throws SQLException {
        List<InventarioBodega> inventarios = new ArrayList<>();
        
        try (Connection con = conexion.getInstance().createConnection();
             PreparedStatement pst = con.prepareStatement(SQL_GET_INVENTARIO_BY_BODEGA)) {
            
            pst.setInt(1, idBodega);
            
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    inventarios.add(mapResultSetToInventario(rs));
                }
            }
        }
        
        return inventarios;
    }
    
    /**
     * Obtiene el stock consolidado de una variante sumando todas las bodegas.
     * 
     * @param idVariante ID de la variante
     * @return Map con totales: "pares", "cajas", "reservado"
     * @throws SQLException Si hay error en la consulta
     */
    public Map<String, Integer> getStockConsolidado(int idVariante) throws SQLException {
        Map<String, Integer> stockMap = new HashMap<>();
        stockMap.put("pares", 0);
        stockMap.put("cajas", 0);
        stockMap.put("reservado", 0);
        
        try (Connection con = conexion.getInstance().createConnection();
             PreparedStatement pst = con.prepareStatement(SQL_GET_STOCK_CONSOLIDADO)) {
            
            pst.setInt(1, idVariante);
            
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    stockMap.put("pares", rs.getInt("total_pares"));
                    stockMap.put("cajas", rs.getInt("total_cajas"));
                    stockMap.put("reservado", rs.getInt("total_reservado"));
                }
            }
        }
        
        return stockMap;
    }
    
    /**
     * Obtiene productos con stock bajo en una bodega específica.
     * 
     * @param idBodega ID de la bodega
     * @return Lista de productos con stock por debajo del mínimo
     * @throws SQLException Si hay error en la consulta
     */
    public List<InventarioBodega> getProductosStockBajo(int idBodega) throws SQLException {
        List<InventarioBodega> inventarios = new ArrayList<>();
        
        try (Connection con = conexion.getInstance().createConnection();
             PreparedStatement pst = con.prepareStatement(SQL_GET_PRODUCTOS_STOCK_BAJO)) {
            
            pst.setInt(1, idBodega);
            
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    InventarioBodega inv = new InventarioBodega();
                    inv.setIdInventarioBodega(rs.getInt("id_inventario_bodega"));
                    inv.setIdBodega(rs.getInt("id_bodega"));
                    inv.setNombreBodega(rs.getString("nombre_bodega"));
                    inv.setIdVariante(rs.getInt("id_variante"));
                    inv.setSku(rs.getString("sku"));
                    inv.setNombreProducto(rs.getString("nombre_producto"));
                    inv.setTalla(rs.getString("talla"));
                    inv.setColor(rs.getString("color"));
                    inv.setStockPar(rs.getInt("Stock_par"));
                    inv.setStockMinimo(rs.getInt("stock_minimo_variante"));
                    
                    inventarios.add(inv);
                }
            }
        }
        
        return inventarios;
    }
    
    // ==================== MÉTODOS DE MODIFICACIÓN ====================
    
    private int[] normalizeStockInputs(String tipo, int stockPar, int stockCaja) {
        int sp = Math.max(0, stockPar);
        int sc = Math.max(0, stockCaja);
        if (tipo != null) {
            String t = tipo.trim().toLowerCase();
            if ("par".equals(t)) { sc = 0; }
            else if ("caja".equals(t)) { sp = 0; }
        }
        if (sp > 0 && sc > 0) { sc = 0; }
        return new int[]{sp, sc};
    }

    public int crearOActualizarInventario(int idBodega, int idVariante, int cantidad, String tipo, String ubicacion) throws SQLException {
        int sp = 0;
        int sc = 0;
        if (tipo != null && tipo.trim().equalsIgnoreCase("par")) { sp = cantidad; }
        else { sc = cantidad; }
        int[] vals = normalizeStockInputs(tipo, sp, sc);
        return crearOActualizarInventario(idBodega, idVariante, vals[0], vals[1], ubicacion);
    }

    /**
     * Crea o actualiza un registro de inventario en una bodega.
     * Si ya existe, actualiza el stock. Si no existe, lo crea.
     * 
     * @param idBodega ID de la bodega
     * @param idVariante ID de la variante
     * @param stockPar Stock en pares
     * @param stockCaja Stock en cajas
     * @param ubicacion Ubicación específica (opcional)
     * @return ID del registro de inventario
     * @throws SQLException Si hay error en la operación
     */
    public int crearOActualizarInventario(int idBodega, int idVariante, int stockPar, 
                                         int stockCaja, String ubicacion) throws SQLException {
        int[] vals = normalizeStockInputs(null, stockPar, stockCaja);
        int sp = vals[0];
        int sc = vals[1];
        
        try (Connection con = conexion.getInstance().createConnection();) {
            // Verificar si ya existe
            try (PreparedStatement pstCheck = con.prepareStatement(SQL_CHECK_INVENTARIO_EXISTS)) {
                pstCheck.setInt(1, idBodega);
                pstCheck.setInt(2, idVariante);
                
                try (ResultSet rs = pstCheck.executeQuery()) {
                    if (rs.next()) {
                        int idInventario = rs.getInt("id_inventario_bodega");
                        int stockReservado = rs.getInt("stock_reservado");
                        
                        actualizarStock(idInventario, sp, sc, stockReservado);
                        actualizarUbicacion(idInventario, ubicacion);

                        int cantidadAudit = sp > 0 ? sp : sc;
                        String tipoAudit = sp > 0 ? "MODIFICACION_STOCK_PAR" : "MODIFICACION_STOCK_CAJA";
                        try (PreparedStatement psAud = con.prepareStatement(
                                "INSERT INTO auditoria_trazabilidad (tipo_evento, id_variante, cantidad, id_bodega_destino, fecha_evento, activo) VALUES (?,?,?,?, NOW(), 1)")) {
                            psAud.setString(1, tipoAudit);
                            psAud.setInt(2, idVariante);
                            psAud.setInt(3, cantidadAudit);
                            psAud.setInt(4, idBodega);
                            psAud.executeUpdate();
                        } catch (SQLException ignore) {}
                        
                        return idInventario;
                    }
                }
            }
            
            // No existe, crear nuevo
            try (PreparedStatement pstInsert = con.prepareStatement(SQL_INSERT_INVENTARIO, 
                    Statement.RETURN_GENERATED_KEYS)) {
                
                pstInsert.setInt(1, idBodega);
                pstInsert.setInt(2, idVariante);
                pstInsert.setInt(3, sp);
                pstInsert.setInt(4, sc);
                pstInsert.setInt(5, 0); // stock_reservado inicial
                pstInsert.setString(6, ubicacion);
                
                pstInsert.executeUpdate();
                
                try (ResultSet rsKeys = pstInsert.getGeneratedKeys()) {
                    if (rsKeys.next()) {
                        int cantidadAudit = sp > 0 ? sp : sc;
                        String tipoAudit = sp > 0 ? "CREACION_STOCK_PAR" : "CREACION_STOCK_CAJA";
                        try (PreparedStatement psAud = con.prepareStatement(
                                "INSERT INTO auditoria_trazabilidad (tipo_evento, id_variante, cantidad, id_bodega_destino, fecha_evento, activo) VALUES (?,?,?,?, NOW(), 1)")) {
                            psAud.setString(1, tipoAudit);
                            psAud.setInt(2, idVariante);
                            psAud.setInt(3, cantidadAudit);
                            psAud.setInt(4, idBodega);
                            psAud.executeUpdate();
                        } catch (SQLException ignore) {}
                        return rsKeys.getInt(1);
                    }
                }
            }
        }
        
        throw new SQLException("No se pudo crear el inventario");
    }
    
    /**
     * Actualiza el stock de un registro de inventario.
     * 
     * @param idInventarioBodega ID del registro de inventario
     * @param stockPar Nuevo stock en pares
     * @param stockCaja Nuevo stock en cajas
     * @param stockReservado Stock reservado
     * @throws SQLException Si hay error en la operación
     */
    public void actualizarStock(int idInventarioBodega, int stockPar, int stockCaja, 
                               int stockReservado) throws SQLException {
        
        try (Connection con = conexion.getInstance().createConnection();
             PreparedStatement pst = con.prepareStatement(SQL_UPDATE_STOCK)) {
            
            pst.setInt(1, Math.max(0, stockPar));
            pst.setInt(2, Math.max(0, stockCaja));
            pst.setInt(3, Math.max(0, stockReservado));
            pst.setInt(4, idInventarioBodega);
            
            pst.executeUpdate();
        }
    }
    
    /**
     * Actualiza la ubicación física de un inventario en bodega.
     * 
     * @param idInventarioBodega ID del registro de inventario
     * @param ubicacion Nueva ubicación específica
     * @throws SQLException Si hay error en la operación
     */
    public void actualizarUbicacion(int idInventarioBodega, String ubicacion) throws SQLException {
        try (Connection con = conexion.getInstance().createConnection();
             PreparedStatement pst = con.prepareStatement(SQL_UPDATE_UBICACION)) {
            
            pst.setString(1, ubicacion);
            pst.setInt(2, idInventarioBodega);
            
            pst.executeUpdate();
        }
    }
    
    /**
     * Incrementa el stock de un producto en una bodega.
     * Si no existe, lo crea.
     * Mantiene el stock del otro tipo (Par/Caja) intacto si ya existe.
     * 
     * @param idBodega ID de la bodega
     * @param idVariante ID de la variante
     * @param cantidad Cantidad a agregar (puede ser negativa)
     * @param tipo "Par" o "Caja"
     * @throws SQLException Si hay error
     */
    public void incrementarStock(int idBodega, int idVariante, int cantidad, String tipo) throws SQLException {
        int incPar = 0;
        int incCaja = 0;
        if (tipo != null && tipo.trim().equalsIgnoreCase("par")) { incPar = cantidad; }
        else { incCaja = cantidad; }
        
        try (Connection con = conexion.getInstance().createConnection()) {
            // 1. Verificar si existe
            String checkSql = "SELECT id_inventario_bodega, Stock_par, Stock_caja FROM inventario_bodega WHERE id_bodega=? AND id_variante=? AND activo=1";
            try (PreparedStatement psCheck = con.prepareStatement(checkSql)) {
                psCheck.setInt(1, idBodega);
                psCheck.setInt(2, idVariante);
                try (ResultSet rs = psCheck.executeQuery()) {
                    if (rs.next()) {
                        // UPDATE
                        int idInv = rs.getInt("id_inventario_bodega");
                        int currentPar = rs.getInt("Stock_par");
                        int currentCaja = rs.getInt("Stock_caja");
                        
                        int newPar = Math.max(0, currentPar + incPar);
                        int newCaja = Math.max(0, currentCaja + incCaja);
                        
                        String updateSql = "UPDATE inventario_bodega SET Stock_par=?, Stock_caja=?, fecha_ultimo_movimiento=NOW() WHERE id_inventario_bodega=?";
                        try (PreparedStatement psUpd = con.prepareStatement(updateSql)) {
                            psUpd.setInt(1, newPar);
                            psUpd.setInt(2, newCaja);
                            psUpd.setInt(3, idInv);
                            psUpd.executeUpdate();
                        }
                    } else {
                        // INSERT
                        // Para insert, asumimos que el otro valor es 0
                        int initialPar = Math.max(0, incPar);
                        int initialCaja = Math.max(0, incCaja);
                        
                        String insertSql = "INSERT INTO inventario_bodega (id_bodega, id_variante, Stock_par, Stock_caja, stock_reservado, activo, fecha_ultimo_movimiento) VALUES (?, ?, ?, ?, 0, 1, NOW())";
                        try (PreparedStatement psIns = con.prepareStatement(insertSql)) {
                            psIns.setInt(1, idBodega);
                            psIns.setInt(2, idVariante);
                            psIns.setInt(3, initialPar);
                            psIns.setInt(4, initialCaja);
                            psIns.executeUpdate();
                        }
                    }
                    
                    // AUDIT
                    if (cantidad != 0) {
                        String tipoAudit = (incPar != 0) ? "AJUSTE_STOCK_PAR" : "AJUSTE_STOCK_CAJA";
                        String sqlAudit = "INSERT INTO auditoria_trazabilidad (tipo_evento, id_variante, cantidad, id_bodega_destino, fecha_evento, activo) VALUES (?,?,?,?, NOW(), 1)";
                        try (PreparedStatement psAud = con.prepareStatement(sqlAudit)) {
                            psAud.setString(1, tipoAudit);
                            psAud.setInt(2, idVariante);
                            psAud.setInt(3, cantidad);
                            psAud.setInt(4, idBodega);
                            psAud.executeUpdate();
                        } catch (SQLException ignore) {}
                    }
                }
            }
        }
    }

    public boolean verifyInventoryRouting(int idBodega, int idVariante, String tipoEsperado) throws SQLException {
        try (Connection con = conexion.getInstance().createConnection();
             PreparedStatement ps = con.prepareStatement("SELECT Stock_par, Stock_caja FROM inventario_bodega WHERE id_bodega=? AND id_variante=? AND activo=1")) {
            ps.setInt(1, idBodega);
            ps.setInt(2, idVariante);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return false;
                int sp = rs.getInt(1);
                int sc = rs.getInt(2);
                if (tipoEsperado != null && tipoEsperado.trim().equalsIgnoreCase("par")) {
                    return sp >= 0 && sc == 0;
                } else if (tipoEsperado != null && tipoEsperado.trim().equalsIgnoreCase("caja")) {
                    return sc >= 0 && sp == 0;
                }
                return sp >= 0 || sc >= 0;
            }
        }
    }
    
    /**
     * Transfiere stock entre dos bodegas.
     * Operación atómica que reduce stock en bodega origen y aumenta en destino.
     * 
     * @param idVariante ID de la variante a transferir
     * @param idBodegaOrigen ID de la bodega origen
     * @param idBodegaDestino ID de la bodega destino
     * @param cantidad Cantidad a transferir (en pares)
     * @return true si la transferencia fue exitosa
     * @throws SQLException Si hay error en la operación
     */
    public boolean transferirStock(int idVariante, int idBodegaOrigen, int idBodegaDestino, 
                                  int cantidad) throws SQLException {
        
        Connection con = null;
        try {
           con = conexion.getInstance().createConnection();
            con.setAutoCommit(false); // Iniciar transacción
            
            // 1. Verificar stock en bodega origen
            InventarioBodega invOrigen = getInventarioBodegaEspecifica(con, idBodegaOrigen, idVariante);
            if (invOrigen == null || invOrigen.getStockPar() < cantidad) {
                con.rollback();
                return false;
            }
            
            // 2. Reducir stock en origen
            int nuevoStockOrigen = invOrigen.getStockPar() - cantidad;
            try (PreparedStatement pst = con.prepareStatement(SQL_UPDATE_STOCK)) {
                pst.setInt(1, nuevoStockOrigen);
                pst.setInt(2, invOrigen.getStockCaja());
                pst.setInt(3, invOrigen.getStockReservado());
                pst.setInt(4, invOrigen.getIdInventarioBodega());
                pst.executeUpdate();
            }
            
            // 3. Aumentar stock en destino (o crear si no existe)
            InventarioBodega invDestino = getInventarioBodegaEspecifica(con, idBodegaDestino, idVariante);
            
            if (invDestino != null) {
                // Ya existe, actualizar
                int nuevoStockDestino = invDestino.getStockPar() + cantidad;
                try (PreparedStatement pst = con.prepareStatement(SQL_UPDATE_STOCK)) {
                    pst.setInt(1, nuevoStockDestino);
                    pst.setInt(2, invDestino.getStockCaja());
                    pst.setInt(3, invDestino.getStockReservado());
                    pst.setInt(4, invDestino.getIdInventarioBodega());
                    pst.executeUpdate();
                }
            } else {
                // No existe, crear
                try (PreparedStatement pst = con.prepareStatement(SQL_INSERT_INVENTARIO)) {
                    pst.setInt(1, idBodegaDestino);
                    pst.setInt(2, idVariante);
                    pst.setInt(3, cantidad);
                    pst.setInt(4, 0); // cajas
                    pst.setInt(5, 0); // reservado
                    pst.setString(6, null); // ubicación
                    pst.executeUpdate();
                }
            }
            
            con.commit(); // Confirmar transacción
            return true;
            
        } catch (SQLException e) {
            if (con != null) {
                try {
                    con.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            throw e;
        } finally {
            if (con != null) {
                try {
                    con.setAutoCommit(true);
                    con.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    
    // ==================== MÉTODOS AUXILIARES ====================
    
    /**
     * Mapea un ResultSet a un objeto InventarioBodega.
     * Método privado helper para reutilizar lógica de mapeo.
     * 
     * @param rs ResultSet con los datos
     * @return Objeto InventarioBodega poblado
     * @throws SQLException Si hay error al leer el ResultSet
     */
    private InventarioBodega mapResultSetToInventario(ResultSet rs) throws SQLException {
        InventarioBodega inv = new InventarioBodega();
        
        inv.setIdInventarioBodega(rs.getInt("id_inventario_bodega"));
        inv.setIdBodega(rs.getInt("id_bodega"));
        inv.setNombreBodega(rs.getString("nombre_bodega"));
        inv.setCodigoBodega(rs.getString("codigo_bodega"));
        inv.setIdVariante(rs.getInt("id_variante"));
        inv.setSku(rs.getString("sku"));
        inv.setIdProducto(rs.getInt("id_producto"));
        inv.setNombreProducto(rs.getString("nombre_producto"));
        inv.setCodigoModelo(rs.getString("codigo_modelo"));
        inv.setTalla(rs.getString("talla"));
        inv.setColor(rs.getString("color"));

        //  CORREGIDO: Manejar NULL correctamente para Stock_par y Stock_caja
        int stockPar = rs.getInt("Stock_par");
        inv.setStockPar(rs.wasNull() ? 0 : stockPar);

        int stockCaja = rs.getInt("Stock_caja");
        inv.setStockCaja(rs.wasNull() ? 0 : stockCaja);

        int stockReservado = rs.getInt("stock_reservado");
        inv.setStockReservado(rs.wasNull() ? 0 : stockReservado);
        inv.setUbicacionEspecifica(rs.getString("ubicacion_especifica"));
        
        // Manejo de fechas
        Timestamp tsMovimiento = rs.getTimestamp("fecha_ultimo_movimiento");
        if (tsMovimiento != null) {
            inv.setFechaUltimoMovimiento(tsMovimiento.toLocalDateTime());
        }
        
        Timestamp tsActualizacion = rs.getTimestamp("fecha_actualizacion");
        if (tsActualizacion != null) {
            inv.setFechaActualizacion(tsActualizacion.toLocalDateTime());
        }
        
        inv.setActivo(rs.getBoolean("activo"));
        inv.setPrecioCompra(rs.getBigDecimal("precio_compra"));
        inv.setPrecioVenta(rs.getBigDecimal("precio_venta"));
        inv.setStockMinimo(rs.getInt("stock_minimo_variante"));
        inv.setParesPorCaja(rs.getInt("pares_por_caja"));
        
        return inv;
    }
    
    /**
     * Obtiene un registro específico de inventario usando una conexión existente.
     * Útil para operaciones transaccionales.
     * 
     * @param con Conexión activa
     * @param idBodega ID de la bodega
     * @param idVariante ID de la variante
     * @return Objeto InventarioBodega o null si no existe
     * @throws SQLException Si hay error en la consulta
     */
    private InventarioBodega getInventarioBodegaEspecifica(Connection con, int idBodega, 
                                                          int idVariante) throws SQLException {
        
        try (PreparedStatement pst = con.prepareStatement(SQL_CHECK_INVENTARIO_EXISTS)) {
            pst.setInt(1, idBodega);
            pst.setInt(2, idVariante);
            
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    InventarioBodega inv = new InventarioBodega();
                    inv.setIdInventarioBodega(rs.getInt("id_inventario_bodega"));
                    inv.setIdBodega(idBodega);
                    inv.setIdVariante(idVariante);
                    inv.setStockPar(rs.getInt("Stock_par"));
                    inv.setStockCaja(rs.getInt("Stock_caja"));
                    inv.setStockReservado(rs.getInt("stock_reservado"));
                    return inv;
                }
            }
        }
        
        return null;
    }

    public java.util.List<Object[]> listarUbicacionesAgrupadas() throws SQLException {
        String sql = "SELECT ubicacion_especifica, " +
                     "SUM(COALESCE(Stock_par, 0)) as total_pares, " +
                     "SUM(COALESCE(Stock_caja, 0)) as total_cajas " +
                     "FROM inventario_bodega " +
                     "WHERE ubicacion_especifica IS NOT NULL AND ubicacion_especifica != '' " +
                     "AND activo = 1 " +
                     "GROUP BY ubicacion_especifica " +
                     "ORDER BY ubicacion_especifica";
        
        java.util.List<Object[]> result = new java.util.ArrayList<>();
        try (Connection con = conexion.getInstance().createConnection();
             PreparedStatement pst = con.prepareStatement(sql);
             ResultSet rs = pst.executeQuery()) {
            while (rs.next()) {
                result.add(new Object[]{
                    rs.getString("ubicacion_especifica"),
                    rs.getInt("total_pares"),
                    rs.getInt("total_cajas")
                });
            }
        }
        return result;
    }

    /**
     * Lista todos los productos en una ubicación específica, incluyendo información detallada
     * sobre el stock y variantes.
     * 
     * @param ubicacion Ubicación específica en el inventario
     * @return Lista de objetos InventarioBodega
     * @throws SQLException Si hay error en la consulta
     */
    public java.util.List<InventarioBodega> listarProductosPorUbicacion(String ubicacion) throws SQLException {
        String sql = "SELECT " +
        "    ib.id_inventario_bodega, " +
        "    ib.id_bodega, " +
        "    b.nombre AS nombre_bodega, " +
        "    b.codigo AS codigo_bodega, " +
        "    ib.id_variante, " +
        "    pv.sku, " +
        "    pv.id_producto, " +
        "    p.nombre AS nombre_producto, " +
        "    p.codigo_modelo, " +
        "    t.numero AS talla, " +
        "    c.nombre AS color, " +
        "    ib.Stock_par, " +
        "    ib.Stock_caja, " +
        "    ib.stock_reservado, " +
        "    ib.ubicacion_especifica, " +
        "    ib.fecha_ultimo_movimiento, " +
        "    ib.fecha_actualizacion, " +
        "    ib.activo, " +
        "    pv.precio_compra, " +
        "    pv.precio_venta, " +
        "    pv.stock_minimo_variante, " +
        "    p.pares_por_caja, " +
        "    pv.ean " +
        "FROM inventario_bodega ib " +
        "INNER JOIN bodegas b ON ib.id_bodega = b.id_bodega " +
        "INNER JOIN producto_variantes pv ON ib.id_variante = pv.id_variante " +
        "INNER JOIN productos p ON pv.id_producto = p.id_producto " +
        "INNER JOIN tallas t ON pv.id_talla = t.id_talla " +
        "INNER JOIN colores c ON pv.id_color = c.id_color " +
        "WHERE ib.ubicacion_especifica = ? " +
        "AND ib.activo = 1 " +
        "ORDER BY p.nombre, t.numero, c.nombre";

        java.util.List<InventarioBodega> list = new java.util.ArrayList<>();
        try (Connection con = conexion.getInstance().createConnection();
             PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, ubicacion);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    InventarioBodega inv = mapResultSetToInventario(rs);
                    inv.setEan(rs.getString("ean"));
                    list.add(inv);
                }
            }
        }
        return list;
    }
}

