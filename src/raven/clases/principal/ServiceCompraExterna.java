package raven.clases.principal;

import raven.controlador.principal.conexion;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import raven.controlador.principal.ModelColor;
import raven.controlador.principal.ModelCompraExterna;
import raven.controlador.principal.ModelCompraExterna.EstadoCompra;
import raven.controlador.principal.ModelCompraExternaDetalle;
import raven.controlador.principal.ModelTalla;

/**
 * Servicio para gestión de compras externas.
 *
 * Implementa Repository Pattern + Service Layer.
 *
 * Responsabilidades: - CRUD de compras externas y detalles - Creación de
 * productos genéricos con EAN - Actualización de inventario - Registro en
 * movimientos de caja
 *
 * @author CrisDEV
 * @version 1.1 - Agregada generación de EAN
 */
public class ServiceCompraExterna {

    // ═══════════════════════════════════════════════════════════════════════════
    // CONSTANTES
    // ═══════════════════════════════════════════════════════════════════════════
    /**
     * ID del producto padre para genéricos
     */
    private static final int ID_PRODUCTO_GENERICO = 223;

    /**
     * ID del proveedor genérico
     */
    private static final int ID_PROVEEDOR_GENERICO = 8;

    /**
     * Prefijo para códigos EAN internos (uso interno, no estándar GS1)
     */
    private static final String EAN_PREFIJO_INTERNO = "200";

    /**
     * Random para generación de EAN
     */
    private static final Random random = new Random();

    // ═══════════════════════════════════════════════════════════════════════════
    // SQL COMPRAS EXTERNAS
    // ═══════════════════════════════════════════════════════════════════════════
    private static final String SQL_INSERT_COMPRA = """
        INSERT INTO compras_externas 
        (numero_compra, tienda_proveedor, numero_factura_recibo, id_bodega, 
         id_usuario, subtotal, iva, total, estado, observaciones, fecha_compra)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

    private static final String SQL_INSERT_DETALLE = """
        INSERT INTO compras_externas_detalles 
        (id_compra_externa, id_producto, id_variante, cantidad, precio_unitario, subtotal)
        VALUES (?, ?, ?, ?, ?, ?)
        """;

    private static final String SQL_SELECT_COMPRA = """
        SELECT c.*, b.nombre as nombre_bodega, u.nombre as nombre_usuario
        FROM compras_externas c
        LEFT JOIN bodegas b ON c.id_bodega = b.id_bodega
        LEFT JOIN usuarios u ON c.id_usuario = u.id_usuario
        WHERE c.id_compra_externa = ?
        """;

    private static final String SQL_SELECT_DETALLES = """
        SELECT d.*, p.nombre as nombre_producto, t.numero as talla, col.nombre as color,
               pv.sku, pv.precio_venta, pv.ean
        FROM compras_externas_detalles d
        JOIN producto_variantes pv ON d.id_variante = pv.id_variante
        JOIN productos p ON d.id_producto = p.id_producto
        LEFT JOIN tallas t ON pv.id_talla = t.id_talla
        LEFT JOIN colores col ON pv.id_color = col.id_color
        WHERE d.id_compra_externa = ?
        ORDER BY d.id_detalle_compra_externa
        """;

    private static final String SQL_SIGUIENTE_NUMERO = """
        SELECT COALESCE(MAX(CAST(SUBSTRING(numero_compra, 13) AS UNSIGNED)), 0) + 1 as siguiente
        FROM compras_externas
        WHERE numero_compra LIKE CONCAT('CE-', DATE_FORMAT(NOW(), '%Y%m%d'), '-%')
        """;

    private static final String SQL_INSERT_MOVIMIENTO_CAJA = """
        INSERT INTO caja_movimiento_detalle 
        (id_movimiento_caja, tipo_movimiento, concepto, monto, 
         id_referencia, tipo_referencia, numero_comprobante, id_usuario, observaciones)
        VALUES (?, 'salida_compra_externa', ?, ?, ?, 'compra_externa', ?, ?, ?)
        """;

    // ═══════════════════════════════════════════════════════════════════════════
    // SQL PRODUCTOS GENÉRICOS Y VARIANTES (ACTUALIZADO CON EAN)
    // ═══════════════════════════════════════════════════════════════════════════
    private static final String SQL_INSERT_VARIANTE = """
        INSERT INTO producto_variantes 
        (id_producto, id_talla, id_color, sku, ean, precio_compra, precio_venta, disponible)
        VALUES (?, ?, ?, ?, ?, ?, ?, 1)
        """;

    private static final String SQL_INSERT_GENERICO = """
        INSERT INTO productos_genericos 
        (id_variante, tipo_origen, tienda_origen, id_usuario_crea, observaciones)
        VALUES (?, 'compra_externa', ?, ?, ?)
        """;

    private static final String SQL_BUSCAR_VARIANTE = """
        SELECT id_variante FROM producto_variantes 
        WHERE id_producto = ? AND id_talla = ? AND id_color = ?
        """;

    /**
     * Verificar si EAN ya existe
     */
    private static final String SQL_VERIFICAR_EAN = """
        SELECT COUNT(*) as existe FROM producto_variantes WHERE ean = ?
        """;

    /**
     * Obtener siguiente secuencia para EAN
     */
    private static final String SQL_SIGUIENTE_SECUENCIA_EAN = """
        SELECT COALESCE(MAX(CAST(SUBSTRING(ean, 4, 9) AS UNSIGNED)), 0) + 1 as siguiente
        FROM producto_variantes 
        WHERE ean LIKE '200%' AND LENGTH(ean) = 13
        """;
    private static final String SQL_UPDATE_INVENTARIO = """
INSERT INTO inventario_bodega (id_bodega, id_variante, Stock_par, Stock_caja, fecha_ultimo_movimiento, activo)
VALUES (?, ?, ?, 0, NOW(), 1)
ON DUPLICATE KEY UPDATE
Stock_par = Stock_par + VALUES(Stock_par),
fecha_ultimo_movimiento = NOW(),
activo = 1
""";

    private static final String SQL_INSERT_MOVIMIENTO_INVENTARIO = """
        INSERT INTO inventario_movimientos 
        (id_producto, id_variante, tipo_movimiento, cantidad, cantidad_pares,
         fecha_movimiento, id_referencia, tipo_referencia, id_usuario, observaciones)
        VALUES (?, ?, 'entrada par', ?, ?, NOW(), ?, 'compra_externa', ?, ?)
        """;

    private static final String SQL_LISTAR_TALLAS = """
        SELECT * FROM tallas WHERE activo = 1 ORDER BY 
            CASE genero WHEN 'HOMBRE' THEN 1 WHEN 'MUJER' THEN 2 ELSE 3 END,
            CAST(numero AS DECIMAL(4,1))
        """;

    private static final String SQL_LISTAR_COLORES = """
        SELECT * FROM colores WHERE activo = 1 ORDER BY nombre
        """;

    // ═══════════════════════════════════════════════════════════════════════════
    // MÉTODOS DE GENERACIÓN DE EAN
    // ═══════════════════════════════════════════════════════════════════════════
    /**
     * Genera un código EAN-13 único para uso interno.
     *
     * Formato: 200 + 9 dígitos secuenciales + 1 dígito de control
     *
     * El prefijo 200-299 está reservado para uso interno según GS1, por lo que
     * es seguro usarlo para productos propios.
     *
     * @param conn Conexión activa (para verificar unicidad)
     * @return Código EAN-13 único
     */
    public String generarEAN13(Connection conn) throws SQLException {
        int maxIntentos = 100;

        for (int i = 0; i < maxIntentos; i++) {
            String ean = generarEAN13Interno(conn);

            if (!existeEAN(conn, ean)) {
                System.out.println("SUCCESS  EAN generado: " + ean);
                return ean;
            }
        }

        throw new SQLException("No se pudo generar un EAN único después de " + maxIntentos + " intentos");
    }

    /**
     * Genera EAN-13 usando secuencia de la BD.
     */
    private String generarEAN13Interno(Connection conn) throws SQLException {
        long secuencia;

        try (PreparedStatement ps = conn.prepareStatement(SQL_SIGUIENTE_SECUENCIA_EAN); ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                secuencia = rs.getLong("siguiente");
            } else {
                secuencia = 1;
            }
        }

        // Agregar algo de aleatoriedad para evitar colisiones en concurrencia
        secuencia = secuencia + random.nextInt(10);

        // Formatear: 200 + 9 dígitos
        String sinDigitoControl = String.format("%s%09d", EAN_PREFIJO_INTERNO, secuencia);

        // Calcular dígito de control
        int digitoControl = calcularDigitoControlEAN13(sinDigitoControl);

        return sinDigitoControl + digitoControl;
    }

    /**
     * Genera EAN-13 basado en datos del producto (método alternativo).
     *
     * Formato: 200 + idProducto(3) + idTalla(2) + idColor(2) + random(2) +
     * check
     */
    public String generarEAN13PorProducto(int idProducto, int idTalla, int idColor) {
        // 200 + producto(3 dígitos) + talla(2) + color(2) + random(2) = 12 dígitos
        String base = String.format("%s%03d%02d%02d%02d",
                EAN_PREFIJO_INTERNO,
                idProducto % 1000, // Últimos 3 dígitos del producto
                idTalla % 100, // Últimos 2 dígitos de talla
                idColor % 100, // Últimos 2 dígitos de color
                random.nextInt(100) // 2 dígitos aleatorios
        );

        int digitoControl = calcularDigitoControlEAN13(base);
        return base + digitoControl;
    }

    /**
     * Calcula el dígito de control para EAN-13.
     *
     * Algoritmo estándar GS1: 1. Sumar dígitos en posiciones impares
     * (1,3,5,7,9,11) 2. Sumar dígitos en posiciones pares (2,4,6,8,10,12) × 3
     * 3. Dígito de control = (10 - (suma % 10)) % 10
     *
     * @param codigo12 Los primeros 12 dígitos del EAN
     * @return Dígito de control (0-9)
     */
    public int calcularDigitoControlEAN13(String codigo12) {
        if (codigo12 == null || codigo12.length() != 12) {
            throw new IllegalArgumentException("El código debe tener exactamente 12 dígitos");
        }

        int sumaImpares = 0;  // Posiciones 1,3,5,7,9,11 (índices 0,2,4,6,8,10)
        int sumaPares = 0;    // Posiciones 2,4,6,8,10,12 (índices 1,3,5,7,9,11)

        for (int i = 0; i < 12; i++) {
            int digito = Character.getNumericValue(codigo12.charAt(i));

            if (i % 2 == 0) {
                sumaImpares += digito;
            } else {
                sumaPares += digito;
            }
        }

        int total = sumaImpares + (sumaPares * 3);
        int resto = total % 10;

        return (resto == 0) ? 0 : (10 - resto);
    }

    /**
     * Valida si un código EAN-13 es válido.
     */
    public boolean validarEAN13(String ean) {
        if (ean == null || ean.length() != 13) {
            return false;
        }

        // Verificar que solo contenga dígitos
        if (!ean.matches("\\d{13}")) {
            return false;
        }

        // Verificar dígito de control
        String codigo12 = ean.substring(0, 12);
        int digitoEsperado = calcularDigitoControlEAN13(codigo12);
        int digitoActual = Character.getNumericValue(ean.charAt(12));

        return digitoEsperado == digitoActual;
    }

    /**
     * Verifica si un EAN ya existe en la base de datos.
     */
    private boolean existeEAN(Connection conn, String ean) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SQL_VERIFICAR_EAN)) {
            ps.setString(1, ean);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("existe") > 0;
                }
            }
        }
        return false;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // MÉTODOS PRINCIPALES
    // ═══════════════════════════════════════════════════════════════════════════
    /**
     * Registra una compra externa completa.
     */
    public int registrarCompra(ModelCompraExterna compra, int idMovimientoCaja) throws SQLException {
        Connection conn = null;

        try {
            conn = conexion.getConnectionStatic();
            conn.setAutoCommit(false);
            
            int idCompra = registrarCompra(conn, compra, idMovimientoCaja);
            
            conn.commit();
            return idCompra;

        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                    System.err.println("WARNING  Rollback ejecutado");
                } catch (SQLException ex) {
                    System.err.println("Error en rollback: " + ex.getMessage());
                }
            }
            throw e;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    /* ignore */ }
            }
        }
    }

    /**
     * Registra una compra externa usando una conexión existente (Transaccional).
     * No hace commit ni cierra la conexión.
     */
    public int registrarCompra(Connection conn, ModelCompraExterna compra, int idMovimientoCaja) throws SQLException {
        // 1. Validar compra
        compra.validarParaGuardar();

        // 2. Generar número de compra
        String numeroCompra = generarNumeroCompra(conn);
        compra.setNumeroCompra(numeroCompra);

        // 3. Insertar cabecera
        int idCompra = insertarCabecera(conn, compra);
        compra.setIdCompraExterna(idCompra);

        // 4. Procesar cada detalle
        for (ModelCompraExternaDetalle detalle : compra.getDetalles()) {
            procesarDetalle(conn, compra, detalle);
        }

        // 5. Registrar salida en caja
        registrarMovimientoCaja(conn, compra, idMovimientoCaja);

        // 6. Marcar como recibida
        marcarRecibida(conn, idCompra);

        System.out.println("SUCCESS  Compra externa registrada: " + numeroCompra + " - $" + compra.getTotal());

        return idCompra;
    }

    /**
     * Obtiene una compra externa con sus detalles.
     */
    public Optional<ModelCompraExterna> obtenerCompra(int idCompra) throws SQLException {
        try (Connection conn = conexion.getConnectionStatic()) {

            ModelCompraExterna compra = null;
            try (PreparedStatement ps = conn.prepareStatement(SQL_SELECT_COMPRA)) {
                ps.setInt(1, idCompra);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        compra = mapearCompra(rs);
                    }
                }
            }

            if (compra == null) {
                return Optional.empty();
            }

            List<ModelCompraExternaDetalle> detalles = new ArrayList<>();
            try (PreparedStatement ps = conn.prepareStatement(SQL_SELECT_DETALLES)) {
                ps.setInt(1, idCompra);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        detalles.add(mapearDetalle(rs));
                    }
                }
            }
            compra.setDetalles(detalles);

            return Optional.of(compra);
        }
    }

    /**
     * Lista compras externas por rango de fechas y bodega.
     */
    public List<ModelCompraExterna> listarCompras(LocalDate fechaInicio, LocalDate fechaFin,
            int idBodega) throws SQLException {
        String sql = """
            SELECT c.*, b.nombre as nombre_bodega, u.nombre as nombre_usuario
            FROM compras_externas c
            LEFT JOIN bodegas b ON c.id_bodega = b.id_bodega
            LEFT JOIN usuarios u ON c.id_usuario = u.id_usuario
            WHERE DATE(c.fecha_compra) BETWEEN ? AND ?
            AND c.id_bodega = ?
            ORDER BY c.fecha_compra DESC
            """;

        List<ModelCompraExterna> compras = new ArrayList<>();

        try (Connection conn = conexion.getConnectionStatic(); PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setDate(1, Date.valueOf(fechaInicio));
            ps.setDate(2, Date.valueOf(fechaFin));
            ps.setInt(3, idBodega);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    compras.add(mapearCompra(rs));
                }
            }
        }

        return compras;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // MÉTODOS DE CATÁLOGOS
    // ═══════════════════════════════════════════════════════════════════════════
    public List<ModelTalla> listarTallas() throws SQLException {
        List<ModelTalla> tallas = new ArrayList<>();

        try (Connection conn = conexion.getConnectionStatic(); PreparedStatement ps = conn.prepareStatement(SQL_LISTAR_TALLAS); ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                ModelTalla talla = new ModelTalla();
                talla.setIdTalla(rs.getInt("id_talla"));
                talla.setNumero(rs.getString("numero"));
                talla.setSistemaFromString(rs.getString("sistema"));
                talla.setGeneroFromString(rs.getString("genero"));
                talla.setActivo(rs.getBoolean("activo"));
                tallas.add(talla);
            }
        }

        return tallas;
    }

    public List<ModelColor> listarColores() throws SQLException {
        List<ModelColor> colores = new ArrayList<>();

        try (Connection conn = conexion.getConnectionStatic(); PreparedStatement ps = conn.prepareStatement(SQL_LISTAR_COLORES); ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                ModelColor color = new ModelColor();
                color.setIdColor(rs.getInt("id_color"));
                color.setNombre(rs.getString("nombre"));
                color.setCodigoHex(rs.getString("codigo_hex"));
                color.setActivo(rs.getBoolean("activo"));
                colores.add(color);
            }
        }

        return colores;
    }

    /**
     * Busca una variante existente o la crea si no existe. ACTUALIZADO: Ahora
     * genera EAN automáticamente.
     */
    public int obtenerOCrearVariante(Connection conn, int idTalla, int idColor,
            BigDecimal precioCompra, BigDecimal precioVenta,
            String tiendaOrigen, int idUsuario,String descripcionCompra) throws SQLException {

        // 1. Buscar variante existente
        try (PreparedStatement ps = conn.prepareStatement(SQL_BUSCAR_VARIANTE)) {
            ps.setInt(1, ID_PRODUCTO_GENERICO);
            ps.setInt(2, idTalla);
            ps.setInt(3, idColor);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int idVariante = rs.getInt("id_variante");
                    // System.out.println("INFO Variante existente encontrada: " + idVariante);
                    return idVariante;
                }
            }
        }

        // 2. Si no existe, crear nueva (con reintentos por colisión de EAN)
        int maxIntentos = 5;
        for (int i = 0; i < maxIntentos; i++) {
            // Generar SKU y EAN
            String sku = generarSkuGenerico(idTalla, idColor);
            String ean = generarEAN13(conn); // Puede lanzar SQLException si no encuentra uno único

            try (PreparedStatement ps = conn.prepareStatement(SQL_INSERT_VARIANTE, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, ID_PRODUCTO_GENERICO);
                ps.setInt(2, idTalla);
                ps.setInt(3, idColor);
                ps.setString(4, sku);
                ps.setString(5, ean);
                ps.setBigDecimal(6, precioCompra);
                ps.setBigDecimal(7, precioVenta);

                int affectedRows = ps.executeUpdate();

                if (affectedRows == 0) {
                    throw new SQLException("La creación de la variante falló, no se agregaron filas.");
                }

                try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        int idVariante = generatedKeys.getInt(1);

                        // Crear registro en productos_genericos
                        registrarProductoGenerico(conn, idVariante, tiendaOrigen, idUsuario, descripcionCompra);

                        return idVariante;
                    } else {
                        throw new SQLException("La creación de la variante falló, no se obtuvo el ID.");
                    }
                }
            } catch (SQLException e) {
                // Verificar si es error de duplicado por EAN
                String msg = e.getMessage().toLowerCase();
                if (msg.contains("duplicate entry") && msg.contains("ean")) {
                    System.out.println("WARNING  Colisión de EAN (" + ean + "), reintentando... (Intento " + (i + 1) + ")");
                    // Continuar al siguiente intento del bucle
                    continue;
                }
                // Si es otro error, lanzarlo
                throw e;
            }
        }
        
        throw new SQLException("No se pudo crear la variante genérica después de varios intentos (posible colisión de EAN)");
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // MÉTODOS PRIVADOS
    // ═══════════════════════════════════════════════════════════════════════════
    private String generarNumeroCompra(Connection conn) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SQL_SIGUIENTE_NUMERO); ResultSet rs = ps.executeQuery()) {

            int siguiente = 1;
            if (rs.next()) {
                siguiente = rs.getInt("siguiente");
            }

            return ModelCompraExterna.generarNumeroCompra(siguiente);
        }
    }

    private int insertarCabecera(Connection conn, ModelCompraExterna compra) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SQL_INSERT_COMPRA, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, compra.getNumeroCompra());
            ps.setString(2, compra.getTiendaProveedor());
            ps.setString(3, compra.getNumeroFacturaRecibo());
            ps.setInt(4, compra.getIdBodega());
            ps.setInt(5, compra.getIdUsuario());
            ps.setBigDecimal(6, compra.getSubtotal());
            ps.setBigDecimal(7, compra.getIva());
            ps.setBigDecimal(8, compra.getTotal());
            ps.setString(9, EstadoCompra.PENDIENTE.getValor());
            ps.setString(10, compra.getObservaciones());
            ps.setTimestamp(11, Timestamp.valueOf(compra.getFechaCompra()));

            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        throw new SQLException("No se pudo obtener ID de compra insertada");
    }

    private void procesarDetalle(Connection conn, ModelCompraExterna compra,
            ModelCompraExternaDetalle detalle) throws SQLException {
        detalle.setIdCompraExterna(compra.getIdCompraExterna());
        detalle.setIdProducto(ID_PRODUCTO_GENERICO);
        System.out.println("Caja Procesando detalle:");
        System.out.println("   - ID Compra: " + detalle.getIdCompraExterna());
        System.out.println("   - ID Producto: " + detalle.getIdProducto());
        System.out.println("   - ID Variante: " + detalle.getIdVariante());
        try (PreparedStatement ps = conn.prepareStatement(SQL_INSERT_DETALLE, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, detalle.getIdCompraExterna());
            ps.setInt(2, detalle.getIdProducto());
            ps.setInt(3, detalle.getIdVariante());
            ps.setInt(4, detalle.getCantidad());
            ps.setBigDecimal(5, detalle.getPrecioUnitario());
            ps.setBigDecimal(6, detalle.getSubtotal());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    detalle.setIdDetalleCompraExterna(rs.getInt(1));
                    System.out.println("   SUCCESS  Detalle insertado: ID " + detalle.getIdDetalleCompraExterna());
                }
            }
        }

        actualizarInventario(conn, compra.getIdBodega(), detalle, compra.getIdUsuario());
    }

    private void actualizarInventario(Connection conn, int idBodega,
            ModelCompraExternaDetalle detalle,
            int idUsuario) throws SQLException {

        try (PreparedStatement ps = conn.prepareStatement(SQL_UPDATE_INVENTARIO)) {
            ps.setInt(1, idBodega);
            ps.setInt(2, detalle.getIdVariante());
            ps.setInt(3, detalle.getCantidad());
            ps.executeUpdate();
        }

        try (PreparedStatement ps = conn.prepareStatement(SQL_INSERT_MOVIMIENTO_INVENTARIO)) {
            ps.setInt(1, detalle.getIdProducto());
            ps.setInt(2, detalle.getIdVariante());
            ps.setInt(3, detalle.getCantidad());
            ps.setInt(4, detalle.getCantidad());
            ps.setInt(5, detalle.getIdCompraExterna());
            ps.setInt(6, idUsuario);
            ps.setString(7, "Compra externa: " + detalle.getDescripcionProducto());
            ps.executeUpdate();
        }
    }

    private void registrarMovimientoCaja(Connection conn, ModelCompraExterna compra,
            int idMovimientoCaja) throws SQLException {

        String concepto = String.format("Compra externa: %s - %s",
                compra.getTiendaProveedor(),
                compra.getNumeroCompra());

        try (PreparedStatement ps = conn.prepareStatement(SQL_INSERT_MOVIMIENTO_CAJA)) {
            ps.setInt(1, idMovimientoCaja);
            ps.setString(2, concepto);
            ps.setBigDecimal(3, compra.getTotal());
            ps.setInt(4, compra.getIdCompraExterna());
            ps.setString(5, compra.getNumeroCompra());
            ps.setInt(6, compra.getIdUsuario());
            ps.setString(7, compra.getObservaciones());
            ps.executeUpdate();
        }
    }

    private void registrarProductoGenerico(Connection conn, int idVariante,
            String tiendaOrigen, int idUsuario,
            String descripcionCompra) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO productos_genericos (id_variante, tipo_origen, tienda_origen, id_usuario_crea, descripcion_compra, observaciones) "
                + "VALUES (?, 'compra_externa', ?, ?, ?, ?)"
        )) {
            ps.setInt(1, idVariante);
            ps.setString(2, tiendaOrigen);
            ps.setInt(3, idUsuario);
            ps.setString(4, descripcionCompra);
            ps.setString(5, "Creado desde compra externa");
            ps.executeUpdate();
        }
    }

    private void marcarRecibida(Connection conn, int idCompra) throws SQLException {
        String sql = "UPDATE compras_externas SET estado = 'recibida', fecha_recepcion = NOW() WHERE id_compra_externa = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idCompra);
            ps.executeUpdate();
        }
    }

    private String generarSkuGenerico(int idTalla, int idColor) {
        return String.format("GEN-%d-%d-%d", ID_PRODUCTO_GENERICO, idTalla, idColor);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // MÉTODOS DE MAPEO
    // ═══════════════════════════════════════════════════════════════════════════
    private ModelCompraExterna mapearCompra(ResultSet rs) throws SQLException {
        ModelCompraExterna compra = new ModelCompraExterna();

        compra.setIdCompraExterna(rs.getInt("id_compra_externa"));
        compra.setNumeroCompra(rs.getString("numero_compra"));
        compra.setTiendaProveedor(rs.getString("tienda_proveedor"));
        compra.setNumeroFacturaRecibo(rs.getString("numero_factura_recibo"));
        compra.setIdBodega(rs.getInt("id_bodega"));
        compra.setIdUsuario(rs.getInt("id_usuario"));
        compra.setSubtotal(rs.getBigDecimal("subtotal"));
        compra.setIva(rs.getBigDecimal("iva"));
        compra.setTotal(rs.getBigDecimal("total"));
        compra.setEstadoFromString(rs.getString("estado"));
        compra.setObservaciones(rs.getString("observaciones"));

        Timestamp tsCompra = rs.getTimestamp("fecha_compra");
        if (tsCompra != null) {
            compra.setFechaCompra(tsCompra.toLocalDateTime());
        }

        Timestamp tsRecepcion = rs.getTimestamp("fecha_recepcion");
        if (tsRecepcion != null) {
            compra.setFechaRecepcion(tsRecepcion.toLocalDateTime());
        }

        try {
            compra.setNombreBodega(rs.getString("nombre_bodega"));
            compra.setNombreUsuario(rs.getString("nombre_usuario"));
        } catch (SQLException e) {
            /* Columnas opcionales */ }

        return compra;
    }

    private ModelCompraExternaDetalle mapearDetalle(ResultSet rs) throws SQLException {
        ModelCompraExternaDetalle detalle = new ModelCompraExternaDetalle();

        detalle.setIdDetalleCompraExterna(rs.getInt("id_detalle_compra_externa"));
        detalle.setIdCompraExterna(rs.getInt("id_compra_externa"));
        detalle.setIdProducto(rs.getInt("id_producto"));
        detalle.setIdVariante(rs.getInt("id_variante"));
        detalle.setCantidad(rs.getInt("cantidad"));
        detalle.setPrecioUnitario(rs.getBigDecimal("precio_unitario"));
        detalle.setSubtotal(rs.getBigDecimal("subtotal"));

        try {
            detalle.setDescripcionProducto(rs.getString("nombre_producto"));
            detalle.setNombreTalla(rs.getString("talla"));
            detalle.setNombreColor(rs.getString("color"));
            detalle.setSku(rs.getString("sku"));
            detalle.setPrecioVenta(rs.getBigDecimal("precio_venta"));
            detalle.setEan(rs.getString("ean"));
        } catch (SQLException e) {
            /* Columnas opcionales */ }

        return detalle;
    }
}

