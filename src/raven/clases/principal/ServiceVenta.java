package raven.clases.principal;

import java.math.BigDecimal;
import java.math.RoundingMode;
import raven.clases.admin.ServiceCajaMovimiento;
import raven.controlador.principal.conexion;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.JOptionPane;
import raven.clases.admin.ServiceUser;
import raven.clases.comercial.ServiceCliente;
import raven.clases.productos.ServiceProduct;
import raven.clases.reportes.ReporteVentas;
import raven.controlador.admin.ModelCaja;
import raven.controlador.admin.ModelCajaMovimiento;
import raven.controlador.admin.ModelUser;
import raven.controlador.comercial.ModelCliente;
import raven.controlador.principal.ModelDetalleVenta;
import raven.controlador.principal.ModelVenta;
import raven.controlador.productos.ModelProduct;
import java.util.logging.Level;
import java.util.logging.Logger;
import raven.clases.comercial.VarianteInfo;
import raven.clases.productos.ServiceProductVariant;
import raven.controlador.comercial.ModelSupplier;
import raven.controlador.productos.ModelBrand;
import raven.controlador.productos.ModelCategory;
import raven.controlador.productos.ModelProductVariant;
import raven.controlador.productos.ModelProfile;

public class ServiceVenta {
    // Dentro de la clase como campo estático

    private static final Logger LOGGER = Logger.getLogger(ServiceVenta.class.getName());
    private final ServiceCajaMovimiento serviceCajaMovimiento = new ServiceCajaMovimiento();
    private final ServiceDetalleVenta serviceDetalle = new ServiceDetalleVenta();
    private final ServiceProduct serviceProduct = new ServiceProduct(); // Añadir esta línea
    private final ReporteVentas reporteVentas = new ReporteVentas();

    public void crearVenta(ModelVenta venta) throws SQLException {
        Connection con = null;
        try {
            con = conexion.getInstance().createConnection();
            con.setAutoCommit(false); // Iniciar transacción
            // Verificar caja abierta
            ModelCajaMovimiento cajaMovimiento = serviceCajaMovimiento
                    .obtenerMovimientoAbierto(venta.getCaja().getIdCaja());
            if (cajaMovimiento == null) {
                throw new SQLException("No hay una caja abierta para realizar la venta");
            }
            venta.setMovimiento(cajaMovimiento);
            // Insertar venta
            String sqlVenta = "INSERT INTO ventas (id_cliente, id_usuario, id_caja, id_movimiento, fecha_venta, subtotal, descuento, iva, total, estado, tipo_pago, observaciones) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement pVenta = con.prepareStatement(sqlVenta, Statement.RETURN_GENERATED_KEYS)) {
                pVenta.setObject(1, venta.getCliente() != null ? venta.getCliente().getIdCliente() : null);
                pVenta.setInt(2, venta.getUsuario().getIdUsuario());
                pVenta.setInt(3, venta.getCaja().getIdCaja());
                pVenta.setInt(4, venta.getMovimiento().getIdMovimiento());
                pVenta.setTimestamp(5, Timestamp.valueOf(venta.getFechaVenta()));
                pVenta.setDouble(6, venta.getSubtotal());
                pVenta.setDouble(7, venta.getDescuento());
                pVenta.setDouble(8, venta.getIva());
                pVenta.setDouble(9, venta.getTotal());
                pVenta.setString(10, venta.getEstado());
                pVenta.setString(11, venta.getTipoPago());
                pVenta.setString(12, venta.getObservaciones());
                pVenta.executeUpdate();

                // Obtener ID generado
                try (ResultSet rs = pVenta.getGeneratedKeys()) {
                    if (rs.next()) {
                        venta.setIdVenta(rs.getInt(1));
                    }
                }
            }

            // Insertar detalles
            for (ModelDetalleVenta detalle : venta.getDetalles()) {
                // Asignar venta al detalle antes de crearlo
                detalle.setVenta(venta);

                // Crear detalle y obtener su ID generado
                int idDetalle = serviceDetalle.crearDetalle(detalle, con);

                // Construir tipo_movimiento compatible con el ENUM
                String tipoVenta = detalle.getTipoVenta() != null
                        ? detalle.getTipoVenta().toLowerCase()
                        : "par"; // por defecto
                String tipoMovimientoEnum;
                if (tipoVenta.contains("caja")) {
                    tipoMovimientoEnum = "salida caja";
                } else {
                    tipoMovimientoEnum = "salida par";
                }

                // Registrar movimiento de inventario usando id_detalle como referencia
                registerInventoryMovement(
                        detalle.getProducto().getProductId(),
                        tipoMovimientoEnum, // <- usar el valor del ENUM
                        detalle.getCantidad(),
                        venta.getUsuario().getIdUsuario(),
                        "venta_detalle",
                        idDetalle,
                        con);

                actualizarInventario(detalle, con);
            }

            // Actualizar monto final en caja
            actualizarMontoCaja(venta.getTotal(), cajaMovimiento.getIdMovimiento(), con);

            con.commit();
        } catch (SQLException e) {
            if (con != null) {
                con.rollback();
            }
            JOptionPane.showMessageDialog(null, "Error al crear venta: " + e.getMessage());
            throw e;
        } finally {
            if (con != null) {
                con.setAutoCommit(true);
                con.close();
            }
        }
    }

    public String idVenta() {
        String idv = "";
        String sql = "select max(id_venta) from ventas";
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            con = conexion.getInstance().createConnection();
            ps = con.prepareStatement(sql);
            rs = ps.executeQuery();
            if (rs.next()) {
                idv = rs.getString(1);
            }
        } catch (SQLException e) {
            e.printStackTrace(); // Or handle the exception properly
        } finally {
            try {
                conexion.getInstance().close(rs, ps, con);
            } catch (SQLException e) {
                e.printStackTrace(); // Or handle the exception properly
            }
        }
        return idv;
    }

    // Versión modificada de registerInventoryMovement que acepta Connection
    private void registerInventoryMovement(int productId, String movementType, int quantity,
            int userId, String referenceType, int referenceId,
            Connection con) throws SQLException {

        // 1. Desactivar trigger
        try (Statement stmt = con.createStatement()) {
            stmt.execute("SET @disable_stock_update = 1;");
        }

        String sql = "INSERT INTO inventario_movimientos (id_producto, tipo_movimiento, cantidad, "
                + "fecha_movimiento, id_usuario, tipo_referencia, id_referencia) "
                + "VALUES (?, ?, ?, NOW(), ?, ?, ?)";

        try (PreparedStatement p = con.prepareStatement(sql)) {
            p.execute("SET @disable_stock_update = 1;");
            p.setInt(1, productId);
            p.setString(2, movementType);
            p.setInt(3, quantity);
            p.setInt(4, userId);
            p.setString(5, referenceType);
            p.setInt(6, referenceId);
            p.executeUpdate();
        }
        // 3. Reactivar trigger (opcional, depende del flujo)
        try (Statement stmt = con.createStatement()) {
            stmt.execute("SET @disable_stock_update = NULL;");
        }
    }

    // Descuenta stock en productovariantes/productos e inventario_bodega
    private void actualizarInventario(ModelDetalleVenta detalle, Connection con) throws SQLException {
        String tipoVenta = detalle.getTipoVenta();
        int cantidad = detalle.getCantidad();

        boolean esCaja = tipoVenta != null && tipoVenta.toLowerCase().contains("caja");
        int afectaCajas = esCaja ? cantidad : 0;
        int afectaPares = esCaja ? 0 : cantidad;

        // Bodega del usuario logueado
        Integer idBodegaUsuario = raven.clases.admin.UserSession
                .getInstance()
                .getIdBodegaUsuario();

        // 1) Desactivar trigger global
        try (Statement stmt = con.createStatement()) {
            stmt.execute("SET @disablestockupdate = 1");
        }

        try {
            // =========================================
            // A) ACTUALIZAR PRODUCTO_VARIANTES / PRODUCTOS (tu lógica original)
            // =========================================
            if (detalle.getIdVariante() > 0) {
                String checkColumnsSQL = "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS " +
                        "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'producto_variantes' " +
                        "AND COLUMN_NAME IN ('stock_por_cajas','stock_por_pares','stock')";
                boolean hasStockPorCajas = false;
                boolean hasStockPorPares = false;
                boolean hasStock = false;

                try (Statement checkStmt = con.createStatement();
                        ResultSet rs = checkStmt.executeQuery(checkColumnsSQL)) {
                    while (rs.next()) {
                        String columnName = rs.getString("COLUMN_NAME");
                        if ("stock_por_cajas".equalsIgnoreCase(columnName))
                            hasStockPorCajas = true;
                        else if ("stock_por_pares".equalsIgnoreCase(columnName))
                            hasStockPorPares = true;
                        else if ("stock".equalsIgnoreCase(columnName))
                            hasStock = true;
                    }
                }

                if (hasStockPorCajas || hasStockPorPares || hasStock) {
                    StringBuilder sqlBuilder = new StringBuilder("UPDATE producto_variantes SET ");
                    boolean needsComma = false;

                    if (hasStockPorCajas) {
                        sqlBuilder.append("stock_por_cajas = stock_por_cajas - ?");
                        needsComma = true;
                    }
                    if (hasStockPorPares) {
                        if (needsComma)
                            sqlBuilder.append(", ");
                        sqlBuilder.append("stock_por_pares = stock_por_pares - ?");
                        needsComma = true;
                    }
                    if (hasStock) {
                        if (needsComma)
                            sqlBuilder.append(", ");
                        sqlBuilder.append("stock = stock - ?");
                    }
                    sqlBuilder.append(" WHERE id_variante = ?");

                    try (PreparedStatement p = con.prepareStatement(sqlBuilder.toString())) {
                        int paramIndex = 1;
                        if (hasStockPorCajas) {
                            p.setInt(paramIndex++, afectaCajas);
                        }
                        if (hasStockPorPares) {
                            p.setInt(paramIndex++, afectaPares);
                        }
                        if (hasStock) {
                            p.setInt(paramIndex++, cantidad);
                        }
                        p.setInt(paramIndex, detalle.getIdVariante());
                        p.executeUpdate();
                    }
                } else {
                    System.out.println("No se encontraron columnas de stock en producto_variantes");
                }
            } else {
                // Fallback a productos
                String checkColumnsSQL = "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS " +
                        "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'productos' " +
                        "AND COLUMN_NAME IN ('stock_por_cajas','stock_por_pares')";
                boolean hasStockPorCajas = false;
                boolean hasStockPorPares = false;

                try (Statement checkStmt = con.createStatement();
                        ResultSet rs = checkStmt.executeQuery(checkColumnsSQL)) {
                    while (rs.next()) {
                        String columnName = rs.getString("COLUMN_NAME");
                        if ("stock_por_cajas".equalsIgnoreCase(columnName))
                            hasStockPorCajas = true;
                        else if ("stock_por_pares".equalsIgnoreCase(columnName))
                            hasStockPorPares = true;
                    }
                }

                if (hasStockPorCajas || hasStockPorPares) {
                    StringBuilder sqlBuilder = new StringBuilder("UPDATE productos SET ");
                    boolean needsComma = false;

                    if (hasStockPorCajas) {
                        sqlBuilder.append("stock_por_cajas = stock_por_cajas - ?");
                        needsComma = true;
                    }
                    if (hasStockPorPares) {
                        if (needsComma)
                            sqlBuilder.append(", ");
                        sqlBuilder.append("stock_por_pares = stock_por_pares - ?");
                    }
                    sqlBuilder.append(" WHERE id_producto = ?");

                    try (PreparedStatement p = con.prepareStatement(sqlBuilder.toString())) {
                        int paramIndex = 1;
                        if (hasStockPorCajas) {
                            p.setInt(paramIndex++, afectaCajas);
                        }
                        if (hasStockPorPares) {
                            p.setInt(paramIndex++, afectaPares);
                        }
                        p.setInt(paramIndex, detalle.getProducto().getProductId());
                        p.executeUpdate();
                    }
                } else {
                    System.out.println("No se encontraron columnas de stock en productos");
                }
            }

            // =========================================
            // B) NUEVO: ACTUALIZAR INVENTARIO_BODEGA
            // =========================================
            if (detalle.getIdVariante() > 0 && idBodegaUsuario != null && idBodegaUsuario > 0) {

                String checkIbSql = "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS " +
                        "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'inventario_bodega' " +
                        "AND COLUMN_NAME IN ('Stock_par','Stock_caja')";
                boolean hasStockPar = false;
                boolean hasStockCaja = false;

                try (Statement checkStmt = con.createStatement();
                        ResultSet rs = checkStmt.executeQuery(checkIbSql)) {
                    while (rs.next()) {
                        String col = rs.getString("COLUMN_NAME");
                        if ("Stock_par".equalsIgnoreCase(col))
                            hasStockPar = true;
                        if ("Stock_caja".equalsIgnoreCase(col))
                            hasStockCaja = true;
                    }
                }

                if (hasStockPar || hasStockCaja) {
                    StringBuilder sb = new StringBuilder("UPDATE inventario_bodega SET ");
                    boolean comma = false;

                    if (hasStockPar) {
                        sb.append("Stock_par = GREATEST(0, Stock_par - ?)");
                        comma = true;
                    }
                    if (hasStockCaja) {
                        if (comma)
                            sb.append(", ");
                        sb.append("Stock_caja = GREATEST(0, Stock_caja - ?)");
                    }
                    sb.append(" WHERE id_bodega = ? AND id_variante = ? AND activo = 1");

                    try (PreparedStatement p = con.prepareStatement(sb.toString())) {
                        int idx = 1;
                        if (hasStockPar) {
                            p.setInt(idx++, afectaPares);
                        }
                        if (hasStockCaja) {
                            p.setInt(idx++, afectaCajas);
                        }
                        p.setInt(idx++, idBodegaUsuario);
                        p.setInt(idx, detalle.getIdVariante());

                        int filas = p.executeUpdate();
                        System.out.println("inventario_bodega actualizado - filas: " + filas +
                                " (bodega=" + idBodegaUsuario +
                                ", variante=" + detalle.getIdVariante() + ")");
                    }
                } else {
                    System.out.println("inventario_bodega no tiene columnas Stock_par/Stock_caja");
                }
            } else {
                System.out.println("No se actualiza inventario_bodega: idVariante=" +
                        detalle.getIdVariante() + ", idBodega=" + idBodegaUsuario);
            }

        } catch (SQLException e) {
            System.err.println("Error al actualizar inventario: " + e.getMessage());
            throw e;
        } finally {
            try (Statement stmt = con.createStatement()) {
                stmt.execute("SET @disablestockupdate = NULL");
            }
        }
    }

    public enum TipoVenta {
        CAJA("caja", 24),
        PAR("par", 1);

        private final String nombre;
        private final int factorConversion;

        TipoVenta(String nombre, int factorConversion) {
            this.nombre = nombre;
            this.factorConversion = factorConversion;
        }

        public int getFactorConversion() {
            return factorConversion;
        }

        public static TipoVenta fromString(String text) {
            for (TipoVenta t : TipoVenta.values()) {
                if (t.nombre.equalsIgnoreCase(text)) {
                    return t;
                }
            }
            return PAR; // Valor por defecto
        }
    }

    private void actualizarMontoCaja(double totalVenta, int idMovimiento, Connection con) throws SQLException {
        String sql = "UPDATE caja_movimientos SET monto_final = monto_final + ? WHERE id_movimiento = ?";
        try (PreparedStatement p = con.prepareStatement(sql)) {
            p.setDouble(1, totalVenta);
            p.setInt(2, idMovimiento);
            p.executeUpdate();
        }
    }

    public int guardarCotizacionTemporal(ModelVenta cotizacion) throws SQLException {
        Connection con = null;
        try {
            con = conexion.getInstance().createConnection();
            con.setAutoCommit(false);

            // Obtener valores por defecto para caja y movimiento
            ModelCaja cajaActiva = obtenerCajaActiva(con);
            ModelCajaMovimiento movimientoActivo = serviceCajaMovimiento
                    .obtenerMovimientoAbierto(cajaActiva.getIdCaja());

            if (movimientoActivo == null) {
                // Si no hay movimiento activo, usar valores por defecto
                int idCajaDefault = obtenerCajaPorDefecto();
                int idMovimientoDefault = obtenerMovimientoPorDefecto();

                // Crear un movimiento temporal para la cotización
                cotizacion.setCaja(new ModelCaja());
                cotizacion.getCaja().setIdCaja(idCajaDefault);

                ModelCajaMovimiento movTemp = new ModelCajaMovimiento();
                movTemp.setIdMovimiento(idMovimientoDefault);
                cotizacion.setMovimiento(movTemp);
            } else {
                // Usar el movimiento activo
                cotizacion.setCaja(cajaActiva);
                cotizacion.setMovimiento(movimientoActivo);
            }

            // Preparar la consulta para insertar en la tabla de ventas
            String sqlCotizacion = "INSERT INTO ventas "
                    + "(id_cliente, id_usuario, id_caja, id_movimiento, fecha_venta, "
                    + "subtotal, descuento, iva, total, estado, tipo_pago, observaciones, es_cotizacion) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

            try (PreparedStatement pCotizacion = con.prepareStatement(sqlCotizacion, Statement.RETURN_GENERATED_KEYS)) {
                // Establecer parámetros
                pCotizacion.setObject(1,
                        cotizacion.getCliente() != null ? cotizacion.getCliente().getIdCliente() : null);
                pCotizacion.setInt(2, cotizacion.getUsuario().getIdUsuario());
                pCotizacion.setInt(3, cotizacion.getCaja().getIdCaja());
                pCotizacion.setInt(4, cotizacion.getMovimiento().getIdMovimiento());
                pCotizacion.setTimestamp(5, Timestamp.valueOf(cotizacion.getFechaVenta()));
                pCotizacion.setDouble(6, cotizacion.getSubtotal());
                pCotizacion.setDouble(7, cotizacion.getDescuento());
                pCotizacion.setDouble(8, 0.0); // IVA por defecto para cotización
                pCotizacion.setDouble(9, cotizacion.getTotal());
                pCotizacion.setString(10, "cotizacion"); // Estado inicial
                pCotizacion.setString(11, cotizacion.getTipoPago() != null ? cotizacion.getTipoPago() : "efectivo");
                pCotizacion.setString(12, cotizacion.getObservaciones());
                pCotizacion.setBoolean(13, true); // Marcar como cotización

                pCotizacion.executeUpdate();

                int idCotizacion;
                try (ResultSet rs = pCotizacion.getGeneratedKeys()) {
                    if (rs.next()) {
                        idCotizacion = rs.getInt(1);
                    } else {
                        throw new SQLException("No se pudo obtener el ID de la cotización");
                    }
                }

                // Insertar detalles en venta_detalles Y GUARDAR EL TIPO DE VENTA
                for (ModelDetalleVenta detalle : cotizacion.getDetalles()) {
                    String sqlDetalle = "INSERT INTO venta_detalles "
                            + "(id_venta, id_producto, id_variante, cantidad, precio_unitario, descuento, subtotal, cantidad_cotizacion, tipo_venta) "
                            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

                    try (PreparedStatement pDetalle = con.prepareStatement(sqlDetalle)) {
                        pDetalle.setInt(1, idCotizacion);
                        pDetalle.setInt(2, detalle.getProducto().getProductId());

                        // Persistir id_variante si existe; en caso contrario NULL
                        if (detalle.getIdVariante() > 0) {
                            pDetalle.setInt(3, detalle.getIdVariante());
                        } else {
                            pDetalle.setNull(3, java.sql.Types.INTEGER);
                        }

                        pDetalle.setInt(4, detalle.getCantidad());
                        pDetalle.setDouble(5, detalle.getPrecioUnitario());
                        pDetalle.setDouble(6, detalle.getDescuento());
                        pDetalle.setDouble(7, detalle.getSubtotal());

                        // Calcular cantidad_cotizacion según el tipo de venta
                        if (detalle.getTipoVenta() != null && detalle.getTipoVenta().toLowerCase().contains("caja")) {
                            pDetalle.setInt(8, detalle.getCantidad() * 24); // 24 pares por caja
                        } else {
                            pDetalle.setInt(8, detalle.getCantidad());
                        }

                        // GUARDAR EL TIPO DE VENTA
                        pDetalle.setString(9, detalle.getTipoVenta());

                        pDetalle.executeUpdate();
                    }
                }

                con.commit();
                return idCotizacion;
            }
        } catch (SQLException e) {
            if (con != null) {
                con.rollback();
            }
            throw e;
        } finally {
            if (con != null) {
                con.setAutoCommit(true);
                con.close();
            }
        }
    }

    /**
     * Convierte una cotización aprobada a venta Este método es específico para
     * trabajar con cotizaciones que ya están en estado "cotizacion_aprobada"
     *
     * @param idCotizacion ID de la cotización a convertir
     * @throws SQLException Si ocurre un error en la base de datos
     */
    public void convertirCotizacionAprobadaAVenta(int idCotizacion) throws SQLException {
        Connection con = null;
        try {
            con = conexion.getInstance().createConnection();
            con.setAutoCommit(false);

            // Verificar que la cotización esté en estado aprobado
            String sqlVerificar = "SELECT es_cotizacion, estado FROM ventas WHERE id_venta = ?";
            boolean esCotizacion = false;
            String estado = "";

            try (PreparedStatement stmt = con.prepareStatement(sqlVerificar)) {
                stmt.setInt(1, idCotizacion);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        esCotizacion = rs.getBoolean("es_cotizacion");
                        estado = rs.getString("estado");
                    }
                }
            }

            if (!esCotizacion) {
                throw new SQLException("El documento no es una cotización");
            }

            if (!estado.equals("cotizacion_aprobada")) {
                throw new SQLException("La cotización debe estar en estado 'cotizacion_aprobada' para ser convertida");
            }

            // Obtener la caja activa y un movimiento abierto
            ModelCaja cajaActiva = obtenerCajaActiva(con);
            ModelCajaMovimiento movimientoActivo = serviceCajaMovimiento
                    .obtenerMovimientoAbierto(cajaActiva.getIdCaja());

            if (movimientoActivo == null) {
                throw new SQLException("No hay un movimiento de caja abierto para la caja activa");
            }

            // Obtener datos completos de la cotización
            ModelVenta cotizacion = obtenerCotizacion(idCotizacion, con);

            // Crear una nueva venta basada en la cotización
            String sqlNuevaVenta = "INSERT INTO ventas "
                    + "(id_cliente, id_usuario, id_caja, id_movimiento, fecha_venta, "
                    + "subtotal, descuento, iva, total, estado, tipo_pago, observaciones, es_cotizacion) "
                    + "VALUES (?, ?, ?, ?, NOW(), ?, ?, ?, ?, 'pendiente', ?, ?, false)";

            int nuevaVentaId;
            try (PreparedStatement pVenta = con.prepareStatement(sqlNuevaVenta, Statement.RETURN_GENERATED_KEYS)) {
                pVenta.setInt(1, cotizacion.getCliente().getIdCliente());
                pVenta.setInt(2, cotizacion.getUsuario().getIdUsuario());
                pVenta.setInt(3, cajaActiva.getIdCaja());
                pVenta.setInt(4, movimientoActivo.getIdMovimiento());
                pVenta.setDouble(5, cotizacion.getSubtotal());
                pVenta.setDouble(6, cotizacion.getDescuento());
                pVenta.setDouble(7, 0.0); // IVA
                pVenta.setDouble(8, cotizacion.getTotal());
                pVenta.setString(9, cotizacion.getTipoPago() != null ? cotizacion.getTipoPago() : "efectivo");
                pVenta.setString(10,
                        cotizacion.getObservaciones() + " (Convertido de cotización #" + idCotizacion + ")");

                pVenta.executeUpdate();

                try (ResultSet rs = pVenta.getGeneratedKeys()) {
                    if (rs.next()) {
                        nuevaVentaId = rs.getInt(1);
                    } else {
                        throw new SQLException("No se pudo obtener el ID de la nueva venta");
                    }
                }
            }

            // Copiar los detalles de la cotización a la nueva venta
            String sqlDetalles = "SELECT id_producto, id_variante, cantidad, precio_unitario, descuento, subtotal, tipo_venta "
                    + "FROM venta_detalles WHERE id_venta = ?";

            try (PreparedStatement pSelect = con.prepareStatement(sqlDetalles)) {
                pSelect.setInt(1, idCotizacion);

                try (ResultSet rs = pSelect.executeQuery()) {
                    while (rs.next()) {
                        // Obtener los valores del detalle de cotización
                        int idProducto = rs.getInt("id_producto");
                        int cantidad = rs.getInt("cantidad");
                        double precioUnitario = rs.getDouble("precio_unitario");
                        double descuento = rs.getDouble("descuento");
                        double subtotal = rs.getDouble("subtotal");
                        String tipoVenta = rs.getString("tipo_venta");
                        int idVariante = rs.getInt("id_variante");
                        boolean idVarianteEsNull = rs.wasNull();

                        // Si tipo_venta es NULL, asignar un valor por defecto
                        if (tipoVenta == null) {
                            tipoVenta = "par"; // Valor por defecto
                        }

                        // Insertar cada detalle en la nueva venta
                        String sqlInsertDetalle = "INSERT INTO venta_detalles "
                                + "(id_venta, id_producto, id_variante, cantidad, precio_unitario, descuento, subtotal, tipo_venta) "
                                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

                        try (PreparedStatement pInsert = con.prepareStatement(sqlInsertDetalle,
                                Statement.RETURN_GENERATED_KEYS)) {
                            pInsert.setInt(1, nuevaVentaId);
                            pInsert.setInt(2, idProducto);

                            if (!idVarianteEsNull && idVariante > 0) {
                                pInsert.setInt(3, idVariante);
                            } else {
                                pInsert.setNull(3, java.sql.Types.INTEGER);
                            }

                            pInsert.setInt(4, cantidad);
                            pInsert.setDouble(5, precioUnitario);
                            pInsert.setDouble(6, descuento);
                            pInsert.setDouble(7, subtotal);
                            pInsert.setString(8, tipoVenta);

                            pInsert.executeUpdate();

                            // Obtener el ID del detalle insertado
                            int idDetalle;
                            try (ResultSet rsDetalle = pInsert.getGeneratedKeys()) {
                                if (rsDetalle.next()) {
                                    idDetalle = rsDetalle.getInt(1);
                                } else {
                                    throw new SQLException("No se pudo obtener el ID del detalle");
                                }
                            }

                            // Construir tipo_movimiento correcto
                            String tipoMovimiento = "salida " + tipoVenta;

                            // Registrar el movimiento de inventario
                            registerInventoryMovement(
                                    idProducto,
                                    tipoMovimiento,
                                    cantidad,
                                    cotizacion.getUsuario().getIdUsuario(),
                                    "venta_detalle",
                                    idDetalle,
                                    con);

                            // Actualizar inventario
                            ModelProduct producto = obtenerProducto(idProducto, con);
                            if (producto != null) {
                                ModelDetalleVenta detalleVenta = new ModelDetalleVenta();
                                detalleVenta.setProducto(producto);
                                detalleVenta.setCantidad(cantidad);
                                detalleVenta.setTipoVenta(tipoMovimiento);
                                detalleVenta.setIdVariante(!idVarianteEsNull ? idVariante : 0);

                                actualizarInventario(detalleVenta, con);
                            }
                        }
                    }
                }
            }

            // Actualizar estado de la cotización original
            String sqlUpdateCotizacion = "UPDATE ventas SET "
                    + "estado = 'cotizacion_convertida' "
                    + "WHERE id_venta = ?";

            try (PreparedStatement pUpdate = con.prepareStatement(sqlUpdateCotizacion)) {
                pUpdate.setInt(1, idCotizacion);
                pUpdate.executeUpdate();
            }

            // Actualizar monto final en caja
            actualizarMontoCaja(cotizacion.getTotal(), movimientoActivo.getIdMovimiento(), con);

            con.commit();
        } catch (SQLException e) {
            if (con != null) {
                try {
                    con.rollback();
                    LOGGER.log(Level.SEVERE, "Error al convertir cotización, se realizó rollback", e);
                } catch (SQLException ex) {
                    // Log error
                    LOGGER.log(Level.SEVERE, "Error al hacer rollback", ex);
                }
            }
            throw e;
        } finally {
            if (con != null) {
                try {
                    con.setAutoCommit(true);
                    con.close();
                } catch (SQLException ex) {
                    // Log error
                    LOGGER.log(Level.SEVERE, "Error al cerrar conexión", ex);
                }
            }
        }
    }

    private int obtenerCajaPorDefecto() throws SQLException {
        // Lógica para obtener una caja por defecto
        // Por ejemplo, buscar la primera caja activa
        String sql = "SELECT id_caja FROM cajas WHERE activa = 1 LIMIT 1";
        try (Connection con = conexion.getInstance().createConnection();
                PreparedStatement stmt = con.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery()) {

            if (rs.next()) {
                return rs.getInt("id_caja");
            } else {
                throw new SQLException("No se encontró una caja activa");
            }
        }
    }

    private int obtenerMovimientoPorDefecto() throws SQLException {
        // Lógica para obtener un movimiento de caja por defecto
        // Esto podría ser crear un movimiento temporal o usar uno existente
        // La implementación dependerá de tu lógica de negocio
        String sql = "SELECT id_movimiento FROM caja_movimientos WHERE fecha_cierre IS NULL LIMIT 1";
        try (Connection con = conexion.getInstance().createConnection();
                PreparedStatement stmt = con.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery()) {

            if (rs.next()) {
                return rs.getInt("id_movimiento");
            } else {
                throw new SQLException("No se encontró un movimiento de caja abierto");
            }
        }
    }

    public void convertirCotizacionAVenta(int idCotizacion) throws SQLException {
        Connection con = null;
        try {
            con = conexion.getInstance().createConnection();
            con.setAutoCommit(false);

            // Verificar que la cotización exista y esté aprobada
            String sqlVerificar = "SELECT es_cotizacion, estado FROM ventas WHERE id_venta = ?";
            boolean esCotizacion = false;
            String estado = "";

            try (PreparedStatement stmt = con.prepareStatement(sqlVerificar)) {
                stmt.setInt(1, idCotizacion);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        esCotizacion = rs.getBoolean("es_cotizacion");
                        estado = rs.getString("estado");
                    }
                }
            }

            if (!esCotizacion) {
                throw new SQLException("El documento no es una cotización");
            }

            if (!estado.equals("cotizacion_aprobada")) {
                throw new SQLException("La cotización debe estar en estado 'cotizacion_aprobada' para ser convertida");
            }

            // Obtener la caja activa y un movimiento abierto
            ModelCaja cajaActiva = obtenerCajaActiva(con);
            ModelCajaMovimiento movimientoActivo = serviceCajaMovimiento
                    .obtenerMovimientoAbierto(cajaActiva.getIdCaja());

            if (movimientoActivo == null) {
                throw new SQLException("No hay un movimiento de caja abierto para la caja activa");
            }

            // Obtener datos completos de la cotización
            ModelVenta cotizacion = obtenerCotizacion(idCotizacion, con);

            // Cambiar el estado y configurar la venta
            cotizacion.setEstado("pendiente"); // Primero pendiente, luego será completada
            cotizacion.setEsCotizacion(false);

            // Actualizar el estado de la cotización original
            String sqlUpdateEstado = "UPDATE ventas SET "
                    + "estado = 'cotizacion_convertida', "
                    + "es_cotizacion = false "
                    + "WHERE id_venta = ?";

            try (PreparedStatement pUpdate = con.prepareStatement(sqlUpdateEstado)) {
                pUpdate.setInt(1, idCotizacion);
                pUpdate.executeUpdate();
            }

            // Crear una nueva venta basada en la cotización
            String sqlNuevaVenta = "INSERT INTO ventas "
                    + "(id_cliente, id_usuario, id_caja, id_movimiento, fecha_venta, "
                    + "subtotal, descuento, iva, total, estado, tipo_pago, observaciones, es_cotizacion) "
                    + "VALUES (?, ?, ?, ?, NOW(), ?, ?, ?, ?, 'pendiente', ?, ?, false)";

            int nuevaVentaId;
            try (PreparedStatement pVenta = con.prepareStatement(sqlNuevaVenta, Statement.RETURN_GENERATED_KEYS)) {
                pVenta.setInt(1, cotizacion.getCliente().getIdCliente());
                pVenta.setInt(2, cotizacion.getUsuario().getIdUsuario());
                pVenta.setInt(3, cajaActiva.getIdCaja());
                pVenta.setInt(4, movimientoActivo.getIdMovimiento());
                pVenta.setDouble(5, cotizacion.getSubtotal());
                pVenta.setDouble(6, cotizacion.getDescuento());
                pVenta.setDouble(7, 0.0); // IVA
                pVenta.setDouble(8, cotizacion.getTotal());
                pVenta.setString(9, cotizacion.getTipoPago());
                pVenta.setString(10,
                        cotizacion.getObservaciones() + " (Convertido de cotización #" + idCotizacion + ")");

                pVenta.executeUpdate();

                try (ResultSet rs = pVenta.getGeneratedKeys()) {
                    if (rs.next()) {
                        nuevaVentaId = rs.getInt(1);
                    } else {
                        throw new SQLException("No se pudo obtener el ID de la nueva venta");
                    }
                }
            }

            // Copiar los detalles de la cotización a la nueva venta
            String sqlDetalles = "SELECT id_producto, id_variante, cantidad, precio_unitario, descuento, subtotal, tipo_venta "
                    + "FROM venta_detalles WHERE id_venta = ?";

            try (PreparedStatement pSelect = con.prepareStatement(sqlDetalles)) {
                pSelect.setInt(1, idCotizacion);

                try (ResultSet rs = pSelect.executeQuery()) {
                    while (rs.next()) {
                        int idVariante = rs.getInt("id_variante");
                        boolean idVarianteEsNull = rs.wasNull();
                        // Insertar cada detalle en la nueva venta
                        String sqlInsertDetalle = "INSERT INTO venta_detalles "
                                + "(id_venta, id_producto, id_variante, cantidad, precio_unitario, descuento, subtotal, tipo_venta) "
                                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

                        try (PreparedStatement pInsert = con.prepareStatement(sqlInsertDetalle,
                                Statement.RETURN_GENERATED_KEYS)) {
                            pInsert.setInt(1, nuevaVentaId);
                            pInsert.setInt(2, rs.getInt("id_producto"));
                            if (!idVarianteEsNull && idVariante > 0) {
                                pInsert.setInt(3, idVariante);
                            } else {
                                pInsert.setNull(3, java.sql.Types.INTEGER);
                            }
                            pInsert.setInt(4, rs.getInt("cantidad"));
                            pInsert.setDouble(5, rs.getDouble("precio_unitario"));
                            pInsert.setDouble(6, rs.getDouble("descuento"));
                            pInsert.setDouble(7, rs.getDouble("subtotal"));
                            pInsert.setString(8, rs.getString("tipo_venta"));

                            pInsert.executeUpdate();

                            // Obtener el ID del detalle insertado
                            int idDetalle;
                            try (ResultSet rsDetalle = pInsert.getGeneratedKeys()) {
                                if (rsDetalle.next()) {
                                    idDetalle = rsDetalle.getInt(1);
                                } else {
                                    throw new SQLException("No se pudo obtener el ID del detalle");
                                }
                            }

                            // Registrar el movimiento de inventario
                            int idProducto = rs.getInt("id_producto");
                            String tipoMovimiento = rs.getString("tipo_venta");
                            int cantidad = rs.getInt("cantidad");

                            // Si el tipo de movimiento no incluye "salida", añadir el prefijo
                            if (tipoMovimiento != null && !tipoMovimiento.contains("salida")) {
                                tipoMovimiento = "salida " + tipoMovimiento;
                            }

                            // Registrar el movimiento de inventario y actualizar stock
                            registerInventoryMovement(
                                    idProducto,
                                    tipoMovimiento,
                                    cantidad,
                                    cotizacion.getUsuario().getIdUsuario(),
                                    "venta_detalle",
                                    idDetalle,
                                    con);

                            // Actualizar el inventario
                            ModelProduct producto = obtenerProducto(idProducto, con);
                            ModelDetalleVenta detalle = new ModelDetalleVenta();
                            detalle.setProducto(producto);
                            detalle.setCantidad(cantidad);
                            detalle.setTipoVenta(tipoMovimiento);
                            detalle.setIdVariante(!idVarianteEsNull ? idVariante : 0);

                            actualizarInventario(detalle, con);
                        }
                    }
                }
            }

            // Actualizar monto final en caja
            actualizarMontoCaja(cotizacion.getTotal(), movimientoActivo.getIdMovimiento(), con);

            con.commit();
        } catch (SQLException e) {
            if (con != null) {
                con.rollback();
            }
            throw e;
        } finally {
            if (con != null) {
                con.setAutoCommit(true);
                con.close();
            }
        }
    }

    private ModelCaja obtenerCajaActiva(Connection con) throws SQLException {
        String sql = "SELECT * FROM cajas WHERE activa = 1 LIMIT 1";
        try (PreparedStatement stmt = con.prepareStatement(sql); ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                ModelCaja caja = new ModelCaja();
                caja.setIdCaja(rs.getInt("id_caja"));
                // ... cargar otros campos si es necesario
                return caja;
            } else {
                throw new SQLException("No hay cajas activas disponibles");
            }
        }
    }

    // En la clase ServiceVenta
    private ModelProduct obtenerProducto(int idProducto, Connection con) throws SQLException {
        // Usar ServiceProduct para obtener el producto
        ServiceProduct serviceProduct = new ServiceProduct();
        return (ModelProduct) serviceProduct.getStockByBodegasForProduct(idProducto);
    }

    private ModelCliente obtenerCliente(int idCliente, Connection con) throws SQLException {
        // Usar ServiceProduct para obtener el producto
        ServiceCliente serviceCliente = new ServiceCliente();
        return serviceCliente.obtenerCliente(idCliente, con);
    }

    private ModelUser obtenerUsuario(int username, Connection con) throws SQLException {
        // Usar ServiceProduct para obtener el producto
        ServiceUser serviceUsuer = new ServiceUser();
        return serviceUsuer.obtenerUsuario(username, con);
    }

    private ModelVenta obtenerCotizacion(int idCotizacion, Connection con) throws SQLException {
        ModelVenta venta = new ModelVenta();

        // Consultar datos de la cotización
        String sqlCotizacion = "SELECT * FROM ventas WHERE id_venta = ?";
        try (PreparedStatement pCotizacion = con.prepareStatement(sqlCotizacion)) {
            pCotizacion.setInt(1, idCotizacion);
            try (ResultSet rsCotizacion = pCotizacion.executeQuery()) {
                if (rsCotizacion.next()) {
                    // Mapear campos de cotización a venta
                    venta.setCliente(obtenerCliente(rsCotizacion.getInt("id_cliente"), con));
                    venta.setUsuario(obtenerUsuario(rsCotizacion.getInt("id_usuario"), con));
                    venta.setFechaVenta(rsCotizacion.getTimestamp("fecha_venta").toLocalDateTime());
                    venta.setSubtotal(rsCotizacion.getDouble("subtotal"));
                    venta.setDescuento(rsCotizacion.getDouble("descuento"));
                    venta.setTotal(rsCotizacion.getDouble("total"));
                    venta.setObservaciones(rsCotizacion.getString("observaciones"));
                    venta.setTipoPago(rsCotizacion.getString("tipo_pago"));
                }
            }
        }

        // Obtener detalles de la cotización
        String sqlDetalles = "SELECT * FROM venta_detalles WHERE id_venta = ?";
        List<ModelDetalleVenta> detalles = new ArrayList<>();
        try (PreparedStatement pDetalles = con.prepareStatement(sqlDetalles)) {
            pDetalles.setInt(1, idCotizacion);
            try (ResultSet rsDetalles = pDetalles.executeQuery()) {
                while (rsDetalles.next()) {
                    ModelDetalleVenta detalle = new ModelDetalleVenta();
                    detalle.setProducto(obtenerProducto(rsDetalles.getInt("id_producto"), con));
                    detalle.setCantidad(rsDetalles.getInt("cantidad"));
                    detalle.setPrecioUnitario(rsDetalles.getDouble("precio_unitario"));
                    detalle.setDescuento(rsDetalles.getDouble("descuento"));
                    detalle.setSubtotal(rsDetalles.getDouble("subtotal"));

                    // Obtener tipo de venta si existe
                    String tipoVenta = rsDetalles.getString("tipo_venta");
                    if (tipoVenta != null) {
                        // Limitar la longitud para evitar truncamiento
                        // La columna tipo_venta en la base de datos es VARCHAR(50)
                        if (tipoVenta.length() > 43) {
                            tipoVenta = tipoVenta.substring(0, 43);
                        }
                        detalle.setTipoVenta(tipoVenta);
                    } else {
                        // Asignar un valor por defecto
                        detalle.setTipoVenta("salida par");
                    }

                    detalles.add(detalle);
                }
            }
        }

        venta.setDetalles(detalles);
        return venta;
    }

    /**
     * Busca una venta específica por su ID con todos sus detalles PRINCIPIO
     * SRP: Búsqueda específica y eficiente por clave primaria
     *
     * @param idVenta ID único de la venta
     * @return ModelVenta completo con detalles, o null si no existe
     * @throws SQLException Si hay error de acceso a datos
     */
    /**
     * Busca una venta específica por su ID con todos sus detalles ADAPTADO:
     * Estructura real de base de datos bodega_zapatos
     *
     * @param idVenta ID único de la venta
     * @return ModelVenta completo con detalles, o null si no existe
     * @throws SQLException Si hay error de acceso a datos
     */
    public ModelVenta buscarVentaPorId(int idVenta) throws SQLException {
        String sql = "SELECT "
                + // Campos de la tabla ventas
                "v.id_venta, v.fecha_venta, v.id_cliente, "
                + "v.id_usuario, v.tipo_pago, v.subtotal, v.descuento, "
                + "v.iva, v.total, v.estado, v.observaciones, "
                + // Información del cliente
                "c.nombre as cliente_nombre, c.dni as cliente_dni, "
                + "c.telefono as cliente_telefono, c.email as cliente_email, "
                + // Información del usuario
                "u.nombre as usuario_nombre, u.email as usuario_email "
                + "FROM ventas v "
                + "LEFT JOIN clientes c ON v.id_cliente = c.id_cliente "
                + "LEFT JOIN usuarios u ON v.id_usuario = u.id_usuario "
                + "WHERE v.id_venta = ?";
        // NOTA: Tu tabla ventas NO tiene campo 'activo'

        try (Connection conn = conexion.getInstance().getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, idVenta);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    // MAPEAR VENTA PRINCIPAL
                    ModelVenta venta = mapearVentaDesdeResultSet(rs);

                    // CARGAR DETALLES DE LA VENTA (productos)
                    venta.setDetalles(obtenerDetallesVenta(venta.getIdVenta()));

                    System.out.println(String.format(
                            "SUCCESS  Venta mapeada - ID: %d, Cliente: %s, Total: $%.2f, Productos: %d",
                            venta.getIdVenta(),
                            venta.getCliente() != null ? venta.getCliente().getNombre() : "N/A",
                            venta.getTotal(),
                            venta.getDetalles() != null ? venta.getDetalles().size() : 0));

                    return venta;
                }
            }
        }

        System.out.println("ERROR  No se encontró venta con ID: " + idVenta);
        return null;
    }

    /**
     * Mapea una venta desde ResultSet a ModelVenta PRINCIPIO DRY: Centraliza
     * lógica de mapeo
     *
     * @param rs ResultSet posicionado en la fila a mapear
     * @return ModelVenta con datos básicos mapeados
     * @throws SQLException Si hay error leyendo datos
     */
    /**
     * Mapea una venta desde ResultSet a ModelVenta ADAPTADO: Tipos double y
     * estructura de ModelVenta
     *
     * @param rs ResultSet posicionado en la fila a mapear
     * @return ModelVenta con datos básicos mapeados
     * @throws SQLException Si hay error leyendo datos
     */
    private ModelVenta mapearVentaDesdeResultSet(ResultSet rs) throws SQLException {
        ModelVenta venta = new ModelVenta();

        // DATOS PRINCIPALES DE LA VENTA
        venta.setIdVenta(rs.getInt("id_venta"));

        // FECHA DE VENTA - Usar TIMESTAMP para incluir hora
        java.sql.Timestamp fechaVenta = rs.getTimestamp("fecha_venta");
        if (fechaVenta != null) {
            // Convertir java.sql.Timestamp a LocalDateTime (preserva hora)
            venta.setFechaVenta(fechaVenta.toLocalDateTime());
        }

        // INFORMACIÓN FINANCIERA - Usar double (tu modelo usa double)
        venta.setSubtotal(rs.getDouble("subtotal"));
        venta.setDescuento(rs.getDouble("descuento"));
        venta.setIva(rs.getDouble("iva"));
        venta.setTotal(rs.getDouble("total"));

        // ESTADO Y TIPO DE PAGO
        venta.setEstado(rs.getString("estado"));
        venta.setTipoPago(rs.getString("tipo_pago"));
        venta.setObservaciones(rs.getString("observaciones"));

        // MAPEAR CLIENTE
        int idCliente = rs.getInt("id_cliente");
        if (idCliente > 0 && !rs.wasNull()) {
            ModelCliente cliente = new ModelCliente();
            cliente.setIdCliente(idCliente);
            cliente.setNombre(rs.getString("cliente_nombre"));
            cliente.setDni(rs.getString("cliente_dni"));
            cliente.setTelefono(rs.getString("cliente_telefono"));
            cliente.setEmail(rs.getString("cliente_email"));
            venta.setCliente(cliente);
        }

        // MAPEAR USUARIO
        int idUsuario = rs.getInt("id_usuario");
        if (idUsuario > 0) {
            ModelUser usuario = new ModelUser();
            usuario.setIdUsuario(idUsuario);
            usuario.setNombre(rs.getString("usuario_nombre"));
            usuario.setEmail(rs.getString("usuario_email"));
            venta.setUsuario(usuario);
        }

        return venta;
    }

    /**
     * Obtiene los detalles (productos) de una venta específica ADAPTADO:
     * Compatible con ModelDetalleVenta existente
     *
     * @param idVenta ID de la venta
     * @return Lista de detalles de venta
     * @throws SQLException Si hay error de acceso a datos
     */
    /**
     * Obtiene los detalles (productos) de una venta específica ADAPTADO:
     * Compatible con ModelDetalleVenta, ModelProduct y ModelProductVariant
     *
     * @param idVenta ID de la venta
     * @return Lista de detalles de venta con productos completamente mapeados
     * @throws SQLException Si hay error de acceso a datos
     */
    /**
     * Obtiene los detalles (productos) de una venta específica ADAPTADO:
     * Estructura completa de bodega_zapatos con variantes
     *
     * @param idVenta ID de la venta
     * @return Lista de detalles de venta con productos completamente mapeados
     * @throws SQLException Si hay error de acceso a datos
     */
    private List<ModelDetalleVenta> obtenerDetallesVenta(int idVenta) throws SQLException {
        List<ModelDetalleVenta> detalles = new ArrayList<>();

        // Consulta adaptada a tu estructura real
        String sql = "SELECT "
                + // Campos de venta_detalles
                "vd.id_detalle, vd.id_producto, vd.id_variante, "
                + "vd.cantidad, vd.tipo_venta, vd.precio_unitario, "
                + "vd.descuento, vd.subtotal, "
                + // Campos del producto base
                "p.id_producto, p.codigo_modelo, p.nombre as producto_nombre, "
                + "p.descripcion, p.precio_compra, p.precio_venta, "
                + "p.talla as producto_talla, p.color as producto_color, "
                + "p.genero, p.activo, p.ubicacion, "
                + // Campos de la marca
                "m.id_marca, m.nombre as marca_nombre, "
                + // Campos de la categoría
                "cat.id_categoria, cat.nombre as categoria_nombre, "
                + // Campos del proveedor
                "prov.id_proveedor, prov.nombre as proveedor_nombre, "
                + // Campos de la variante (si existe)
                "pv.id_variante, pv.sku as variante_sku, "
                + "pv.ean as variante_ean, "
                + "COALESCE((SELECT SUM(ib2.Stock_par) FROM inventario_bodega ib2 WHERE ib2.id_variante = pv.id_variante AND ib2.activo = 1), 0) AS stock_por_pares, "
                + "COALESCE((SELECT SUM(ib2.Stock_caja) FROM inventario_bodega ib2 WHERE ib2.id_variante = pv.id_variante AND ib2.activo = 1), 0) AS stock_por_cajas, "
                + "pv.precio_compra as variante_precio_compra, "
                + "pv.precio_venta as variante_precio_venta, "
                + // Información de talla (de la variante)
                "t.id_talla, t.numero as talla_numero, t.sistema as talla_sistema, "
                + // Información de color (de la variante)
                "col.id_color, col.nombre as color_nombre, col.codigo_hex "
                + "FROM venta_detalles vd "
                + "INNER JOIN productos p ON vd.id_producto = p.id_producto "
                + "LEFT JOIN marcas m ON p.id_marca = m.id_marca "
                + "LEFT JOIN categorias cat ON p.id_categoria = cat.id_categoria "
                + "LEFT JOIN proveedores prov ON p.id_proveedor = prov.id_proveedor "
                + // Join con variante solo si existe
                "LEFT JOIN producto_variantes pv ON vd.id_variante = pv.id_variante "
                + "LEFT JOIN tallas t ON pv.id_talla = t.id_talla "
                + "LEFT JOIN colores col ON pv.id_color = col.id_color "
                + "WHERE vd.id_venta = ? "
                + "AND vd.activo = 1 "
                + // Tu tabla tiene campo activo
                "ORDER BY vd.id_detalle";

        try (Connection conn = conexion.getInstance().getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, idVenta);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    // ===================================================
                    // CREAR DETALLE DE VENTA
                    // ===================================================
                    ModelDetalleVenta detalle = new ModelDetalleVenta();

                    detalle.setIdDetalle(rs.getInt("id_detalle"));
                    detalle.setIdVariante(rs.getInt("id_variante"));
                    detalle.setCantidad(rs.getInt("cantidad"));
                    detalle.setTipoVenta(rs.getString("tipo_venta"));
                    detalle.setPrecioUnitario(rs.getDouble("precio_unitario"));
                    detalle.setDescuento(rs.getDouble("descuento"));
                    detalle.setSubtotal(rs.getDouble("subtotal"));

                    // ===================================================
                    // CREAR Y MAPEAR PRODUCTO BASE
                    // ===================================================
                    ModelProduct producto = new ModelProduct();

                    // IDs y códigos
                    producto.setProductId(rs.getInt("id_producto"));
                    producto.setModelCode(rs.getString("codigo_modelo"));

                    // Información básica
                    producto.setName(rs.getString("producto_nombre"));
                    producto.setDescription(rs.getString("descripcion"));
                    producto.setGender(rs.getString("genero"));
                    producto.setActive(rs.getBoolean("activo"));
                    producto.setUbicacion(rs.getString("ubicacion"));

                    // Precios del producto base
                    producto.setPurchasePrice(rs.getDouble("precio_compra"));
                    producto.setSalePrice(rs.getDouble("precio_venta"));

                    // Talla y color del producto base (pueden ser null si usa variantes)
                    producto.setSize(rs.getString("producto_talla"));
                    producto.setColor(rs.getString("producto_color"));

                    // ===================================================
                    // MAPEAR MARCA
                    // ===================================================
                    int idMarca = rs.getInt("id_marca");
                    if (idMarca > 0) {
                        ModelBrand marca = new ModelBrand();
                        marca.setBrandId(idMarca);
                        marca.setName(rs.getString("marca_nombre"));
                        producto.setBrand(marca);
                    }

                    // ===================================================
                    // MAPEAR CATEGORÍA
                    // ===================================================
                    int idCategoria = rs.getInt("id_categoria");
                    if (idCategoria > 0) {
                        ModelCategory categoria = new ModelCategory();
                        categoria.setCategoryId(idCategoria);
                        categoria.setName(rs.getString("categoria_nombre"));
                        producto.setCategory(categoria);
                    }

                    // ===================================================
                    // MAPEAR PROVEEDOR
                    // ===================================================
                    int idProveedor = rs.getInt("id_proveedor");
                    if (idProveedor > 0) {
                        ModelSupplier proveedor = new ModelSupplier();
                        proveedor.setSupplierId(idProveedor);
                        proveedor.setName(rs.getString("proveedor_nombre"));
                        producto.setSupplier(proveedor);
                    }

                    // ===================================================
                    // MAPEAR VARIANTE (si existe)
                    // ===================================================
                    int idVariante = rs.getInt("id_variante");
                    if (idVariante > 0 && !rs.wasNull()) {
                        ModelProductVariant variante = new ModelProductVariant();

                        variante.setVariantId(idVariante);
                        variante.setProductId(producto.getProductId());
                        variante.setSku(rs.getString("variante_sku"));
                        variante.setBarcode(rs.getString("variante_ean"));
                        variante.setEan(rs.getString("variante_ean"));

                        // Stock de la variante
                        variante.setStockPairs(rs.getInt("stock_por_pares"));
                        variante.setStockBoxes(rs.getInt("stock_por_cajas"));

                        // Precios específicos de la variante
                        Double precioCompraVariante = rs.getDouble("variante_precio_compra");
                        if (!rs.wasNull()) {
                            variante.setPurchasePrice(precioCompraVariante);
                        }

                        Double precioVentaVariante = rs.getDouble("variante_precio_venta");
                        if (!rs.wasNull()) {
                            variante.setSalePrice(precioVentaVariante);
                        }

                        // Información de talla
                        int idTalla = rs.getInt("id_talla");
                        if (idTalla > 0 && !rs.wasNull()) {
                            variante.setSizeId(idTalla);
                            variante.setSizeName(rs.getString("talla_numero"));
                            variante.setSizeSystem(rs.getString("talla_sistema"));
                        }

                        // Información de color
                        int idColor = rs.getInt("id_color");
                        if (idColor > 0 && !rs.wasNull()) {
                            variante.setColorId(idColor);
                            variante.setColorName(rs.getString("color_nombre"));
                            variante.setColorHex(rs.getString("codigo_hex"));
                        }

                        // Agregar variante a la lista de variantes del producto
                        List<ModelProductVariant> variantes = new ArrayList<>();
                        variantes.add(variante);
                        producto.setVariants(variantes);

                        System.out.println(String.format(
                                "  → Variante: %s (Talla: %s, Color: %s, Stock: %d pares)",
                                variante.getSku(),
                                variante.getSizeName() != null ? variante.getSizeName() : "N/A",
                                variante.getColorName() != null ? variante.getColorName() : "N/A",
                                variante.getStockPairs()));
                    }

                    // ===================================================
                    // ASOCIAR PRODUCTO AL DETALLE
                    // ===================================================
                    detalle.setProducto(producto);
                    detalles.add(detalle);

                    System.out.println(String.format(
                            "  OK Detalle #%d: %s - Cant: %d - Precio: $%.2f",
                            detalle.getIdDetalle(),
                            producto.getName(),
                            detalle.getCantidad(),
                            detalle.getPrecioUnitario()));
                }
            }
        }

        System.out.println(String.format(
                "SUCCESS  Detalles cargados para venta %d: %d productos",
                idVenta, detalles.size()));

        return detalles;
    }

    /**
     * Crea un ModelDetalleVenta completo desde un ResultSet NUEVO: Construye
     * objetos ModelProduct y ModelProductVariant completos
     *
     * @param rs ResultSet con todos los datos del detalle
     * @return ModelDetalleVenta con producto y variante completos
     * @throws SQLException Si ocurre un error accediendo a los datos
     */
    private ModelDetalleVenta crearDetalleVentaCompleto(ResultSet rs) throws SQLException {
        ModelDetalleVenta detalle = new ModelDetalleVenta();

        // ====================================================================
        // DATOS BÁSICOS DEL DETALLE
        // ====================================================================
        detalle.setIdDetalle(rs.getInt("id_detalle"));
        detalle.setCantidad(rs.getInt("cantidad"));
        detalle.setPrecioUnitario(rs.getDouble("precio_unitario"));
        detalle.setDescuento(rs.getDouble("descuento"));
        detalle.setSubtotal(rs.getDouble("subtotal"));
        detalle.setTipoVenta(rs.getString("tipo_venta"));

        // Manejar id_variante - CRÍTICO para devoluciones
        int idVariante = rs.getInt("id_variante");
        if (!rs.wasNull()) {
            detalle.setIdVariante(idVariante);
        } else {
            detalle.setIdVariante(0);
        }

        // ====================================================================
        // CREAR MODELPRODUCT COMPLETO
        // ====================================================================
        ModelProduct producto = crearModelProductCompleto(rs);

        // ====================================================================
        // CREAR Y AGREGAR VARIANTE SI EXISTE
        // ====================================================================
        if (detalle.getIdVariante() > 0) {
            ModelProductVariant variante = crearModelProductVariantCompleto(rs);

            // Agregar la variante al producto
            if (producto.getVariants() == null) {
                producto.setVariants(new ArrayList<>());
            }
            producto.getVariants().clear(); // Solo mantener la variante específica de esta venta
            producto.getVariants().add(variante);

            // Asignar imagen de la variante al perfil del producto si existe
            if (variante.hasImage()) {
                if (producto.getProfile() == null) {
                    producto.setProfile(new ModelProfile());
                }
                producto.getProfile().setImageBytes(variante.getImageBytes());
            }
        }

        // Asignar el producto completo al detalle
        detalle.setProducto(producto);

        return detalle;
    }

    /**
     * Crea un ModelProduct completo desde un ResultSet NUEVO: Construye el
     * producto con todas sus relaciones
     *
     * @param rs ResultSet con datos del producto
     * @return ModelProduct completo
     * @throws SQLException Si ocurre un error accediendo a los datos
     */
    private ModelProduct crearModelProductCompleto(ResultSet rs) throws SQLException {
        ModelProduct producto = new ModelProduct();

        // ====================================================================
        // DATOS BÁSICOS DEL PRODUCTO
        // ====================================================================
        producto.setProductId(rs.getInt("id_producto"));
        producto.setModelCode(rs.getString("codigo_modelo"));
        producto.setName(rs.getString("producto_nombre"));
        producto.setDescription(rs.getString("producto_descripcion"));
        producto.setGender(rs.getString("genero"));
        producto.setPurchasePrice(rs.getDouble("producto_precio_compra"));
        producto.setSalePrice(rs.getDouble("producto_precio_venta"));
        producto.setUbicacion(rs.getString("ubicacion"));
        producto.setPairsPerBox(rs.getInt("pares_por_caja"));
        producto.setActive(true); // Asumimos que está activo si está en una venta

        // ====================================================================
        // CREAR Y ASIGNAR CATEGORÍA
        // ====================================================================
        if (rs.getObject("id_categoria") != null) {
            ModelCategory categoria = new ModelCategory();
            categoria.setCategoryId(rs.getInt("id_categoria"));
            categoria.setName(rs.getString("categoria_nombre"));
            categoria.setActive(true);
            producto.setCategory(categoria);
        }

        // ====================================================================
        // CREAR Y ASIGNAR MARCA
        // ====================================================================
        if (rs.getObject("id_marca") != null) {
            ModelBrand marca = new ModelBrand();
            marca.setBrandId(rs.getInt("id_marca"));
            marca.setName(rs.getString("marca_nombre"));
            marca.setActive(true);
            producto.setBrand(marca);
        }

        // ====================================================================
        // CREAR Y ASIGNAR PROVEEDOR SI EXISTE
        // ====================================================================
        if (rs.getObject("id_proveedor") != null) {
            ModelSupplier proveedor = new ModelSupplier();
            proveedor.setSupplierId(rs.getInt("id_proveedor"));
            proveedor.setName(rs.getString("proveedor_nombre"));
            proveedor.setActive(true);
            producto.setSupplier(proveedor);
        }

        // ====================================================================
        // INICIALIZAR PERFIL PARA IMÁGENES
        // ====================================================================
        if (producto.getProfile() == null) {
            producto.setProfile(new ModelProfile());
        }

        return producto;
    }

    /**
     * Crea un ModelProductVariant completo desde un ResultSet NUEVO: Construye
     * la variante con toda su información
     *
     * @param rs ResultSet con datos de la variante
     * @return ModelProductVariant completo
     * @throws SQLException Si ocurre un error accediendo a los datos
     */
    private ModelProductVariant crearModelProductVariantCompleto(ResultSet rs) throws SQLException {
        ModelProductVariant variante = new ModelProductVariant();

        // ====================================================================
        // DATOS BÁSICOS DE LA VARIANTE
        // ====================================================================
        variante.setVariantId(rs.getInt("id_variante"));
        variante.setProductId(rs.getInt("id_producto"));
        variante.setSku(rs.getString("sku"));
        variante.setEan(rs.getString("ean"));
        variante.setBarcode(rs.getString("codigo_barras"));
        variante.setAvailable(rs.getBoolean("disponible"));

        // ====================================================================
        // STOCKS
        // ====================================================================
        variante.setStockPairs(rs.getInt("stock_por_pares"));
        variante.setStockBoxes(rs.getInt("stock_por_cajas"));

        if (rs.getObject("stock_minimo_variante") != null) {
            variante.setMinStock(rs.getInt("stock_minimo_variante"));
        }

        // ====================================================================
        // PRECIOS ESPECÍFICOS DE LA VARIANTE
        // ====================================================================
        if (rs.getObject("variante_precio_compra") != null) {
            variante.setPurchasePrice(rs.getDouble("variante_precio_compra"));
        }

        if (rs.getObject("variante_precio_venta") != null) {
            variante.setSalePrice(rs.getDouble("variante_precio_venta"));
        }

        // ====================================================================
        // INFORMACIÓN DE TALLA
        // ====================================================================
        if (rs.getObject("id_talla") != null) {
            variante.setSizeId(rs.getInt("id_talla"));
            String tallaNumero = rs.getString("talla_numero");
            String tallaSistema = rs.getString("talla_sistema");

            if (tallaNumero != null) {
                String nombreCompleto = tallaNumero;
                if (tallaSistema != null && !tallaSistema.isEmpty()) {
                    nombreCompleto += " " + tallaSistema;
                }
                variante.setSizeName(nombreCompleto);
                variante.setSizeSystem(tallaSistema);
            }
        }

        // ====================================================================
        // INFORMACIÓN DE COLOR
        // ====================================================================
        if (rs.getObject("id_color") != null) {
            variante.setColorId(rs.getInt("id_color"));
            variante.setColorName(rs.getString("color_nombre"));
            variante.setColorHex(rs.getString("color_hex"));
        }

        // ====================================================================
        // IMAGEN DE LA VARIANTE
        // ====================================================================
        byte[] imagenBytes = rs.getBytes("imagen");
        if (imagenBytes != null && imagenBytes.length > 0) {
            variante.setImageBytes(imagenBytes);

            System.out.println(" Imagen cargada para variante - ID: " + variante.getVariantId()
                    + " - SKU: " + variante.getSku()
                    + " - Tamaño: " + imagenBytes.length + " bytes");
        }

        return variante;
    }

    /**
     * Obtiene información específica de una variante para devoluciones NUEVO:
     * Método especializado para obtener datos precisos de variantes
     *
     * @param idProducto ID del producto
     * @param idVariante ID de la variante (puede ser 0)
     * @return Información de la variante o del producto base
     * @throws SQLException Si ocurre un error en la base de datos
     */
    public VarianteInfo obtenerInformacionVariante(int idProducto, int idVariante) throws SQLException {
        if (idVariante > 0) {
            // Obtener información específica de la variante
            ServiceProductVariant serviceProductVariant = new ServiceProductVariant();
            int bodega = 1;
            ModelProductVariant variante = (ModelProductVariant) serviceProductVariant.getVariantesByBodega(idVariante,
                    bodega);
            if (variante != null) {
                return new VarianteInfo(
                        variante.getVariantId(),
                        variante.getSku(),
                        variante.getEan(),
                        variante.getSizeName(),
                        variante.getColorName(),
                        variante.getStockPairs(),
                        variante.getStockBoxes(),
                        variante.getSalePrice(),
                        variante.hasImage());
            }
        }

        // Fallback: obtener información del producto base
        ModelProduct producto = (ModelProduct) serviceProduct.getProductsByBodega(idProducto);
        if (producto != null) {
            return new VarianteInfo(
                    0, // Sin variante específica
                    "PROD-" + producto.getProductId(),
                    null,
                    producto.getSize(), // Talla del producto base
                    producto.getColor(), // Color del producto base
                    producto.getPairsStock(),
                    producto.getBoxesStock(),
                    producto.getSalePrice(),
                    producto.getProfile() != null && producto.getProfile().getImageBytes() != null);
        }

        return null;
    }

    /**
     * Obtiene el stock actual de una variante específica ÚTIL para validaciones
     * en tiempo real durante devoluciones
     *
     * @param idProducto ID del producto
     * @param idVariante ID de la variante
     * @return Array con [stockPares, stockCajas] o null si no existe
     * @throws SQLException Si ocurre un error en la base de datos
     */
    public int[] obtenerStockActualVariante(int idProducto, int idVariante) throws SQLException {
        if (idVariante > 0) {
            // Usar el servicio de variantes para obtener stock actual
            ServiceProductVariant serviceProductVariant = new ServiceProductVariant();
            ModelProductVariant variante = serviceProductVariant.getVariantById(idVariante, false);
            if (variante != null) {
                return new int[] { variante.getStockPairs(), variante.getStockBoxes() };
            }
        }

        // Fallback: usar el servicio de productos
        return serviceProduct.obtenerStockVariante(idProducto, null, null);
    }

    /**
     * Obtiene el precio actual de una variante específica ÚTIL para cálculos de
     * devolución con precios actualizados
     *
     * @param idProducto ID del producto
     * @param idVariante ID de la variante
     * @return Precio actual o null si no existe
     * @throws SQLException Si ocurre un error en la base de datos
     */
    /**
     * Obtiene el precio actual de una variante específica CORREGIDO: Instancia
     * el servicio dentro del método
     */
    public BigDecimal obtenerPrecioActualVariante(int idProducto, int idVariante) throws SQLException {
        if (idVariante > 0) {
            // Crear instancia local del servicio
            ServiceProductVariant serviceProductVariant = new ServiceProductVariant();
            ModelProductVariant variante = serviceProductVariant.getVariantById(idVariante, false);

            if (variante != null && variante.getSalePrice() != null) {
                return BigDecimal.valueOf(variante.getSalePrice());
            }
        }

        // Fallback: precio del producto base
        ServiceProduct serviceProduct = new ServiceProduct();
        ModelProduct producto = serviceProduct.getProductById(idProducto);
        if (producto != null) {
            return BigDecimal.valueOf(producto.getSalePrice());
        }

        return null;
    }

    /**
     * Busca ventas con información completa de productos para el sistema de
     * devoluciones MEJORADO: Incluye información de variantes en la búsqueda
     *
     * @param criterio Criterio de búsqueda (puede ser ID, DNI, nombre, o código
     *                 de producto)
     * @return Lista de ventas que coinciden con el criterio
     * @throws SQLException Si ocurre un error en la base de datos
     */
    /**
     * Busca ventas con información completa de productos para el sistema de
     * devoluciones CORREGIDO: Sin modificar ModelVenta
     */
    public List<ModelVenta> buscarVentasParaDevolucionConProductos(String criterio, int idBodega) throws SQLException {
        List<ModelVenta> ventas = new ArrayList<>();
        Connection con = null;

        if (criterio == null || criterio.trim().isEmpty()) {
            throw new IllegalArgumentException("El criterio de búsqueda no puede estar vacío");
        }

        try {
            con = conexion.getInstance().createConnection();

            // CONSULTA CORREGIDA - Uso de alias para coincidir con
            // mapearVentaDesdeResultSet
            String sql = "SELECT DISTINCT v.*, "
                    + "c.nombre as cliente_nombre, "
                    + "c.dni as cliente_dni, "
                    + "c.telefono as cliente_telefono, "
                    + "c.email as cliente_email, " // Intentamos obtener email
                    + "u.nombre as usuario_nombre, "
                    + "u.email as usuario_email, " // Agregado también para usuario
                    + "DATEDIFF(CURDATE(), v.fecha_venta) as dias_transcurridos "
                    + "FROM ventas v "
                    + "LEFT JOIN clientes c ON v.id_cliente = c.id_cliente "
                    + "JOIN usuarios u ON v.id_usuario = u.id_usuario "
                    + "JOIN cajas cj ON v.id_caja = cj.id_caja " // JOIN con cajas para obtener bodega
                    + "LEFT JOIN venta_detalles vd ON v.id_venta = vd.id_venta "
                    + "LEFT JOIN productos p ON vd.id_producto = p.id_producto "
                    + "LEFT JOIN producto_variantes pv ON vd.id_variante = pv.id_variante "
                    + "WHERE v.estado = 'completada' "
                    + "AND cj.id_bodega = ? " // FILTRO POR BODEGA (Corregido)
                    + "AND (v.id_venta = ? OR c.dni LIKE ? OR c.nombre LIKE ? OR "
                    + "     p.codigo_modelo LIKE ? OR p.nombre LIKE ? OR "
                    + "     pv.sku LIKE ? OR pv.ean LIKE ?) "
                    + "AND DATEDIFF(CURDATE(), v.fecha_venta) <= 30 "
                    + "ORDER BY v.fecha_venta DESC "
                    + "LIMIT 50";

            try (PreparedStatement ps = con.prepareStatement(sql)) {
                String criterioBusqueda = "%" + criterio.trim() + "%";

                Integer idVenta = null;
                try {
                    idVenta = Integer.parseInt(criterio.trim());
                } catch (NumberFormatException e) {
                    idVenta = 0; // Si no es número, usar 0
                }

                // Establecer parámetros
                ps.setInt(1, idBodega);
                ps.setInt(2, idVenta);
                ps.setString(3, criterioBusqueda);
                ps.setString(4, criterioBusqueda);
                ps.setString(5, criterioBusqueda);
                ps.setString(6, criterioBusqueda);
                ps.setString(7, criterioBusqueda);
                ps.setString(8, criterioBusqueda);

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        ModelVenta venta = mapearVentaDesdeResultSet(rs);

                        // Calcular elegibilidad para logging
                        int diasTranscurridos = rs.getInt("dias_transcurridos");
                        boolean elegible = diasTranscurridos <= 30;

                        ventas.add(venta);

                        System.out.println("Buscar Venta encontrada - ID: " + venta.getIdVenta()
                                + " - Cliente: "
                                + (venta.getCliente() != null ? venta.getCliente().getNombre() : "Sin cliente")
                                + " - Días: " + diasTranscurridos + " - Elegible: " + elegible);
                    }
                }
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al buscar ventas para devolución. Criterio: " + criterio, e);
            throw new SQLException("Error en la búsqueda: " + e.getMessage(), e);
        } finally {
            if (con != null) {
                try {
                    con.close();
                } catch (SQLException ex) {
                    LOGGER.log(Level.WARNING, "Error al cerrar conexión", ex);
                }
            }
        }

        return ventas;
    }

    /**
     * Clase wrapper para ventas con información de elegibilidad
     */
    public static class VentaConElegibilidad {

        private final ModelVenta venta;
        private final boolean elegible;
        private final int diasTranscurridos;

        public VentaConElegibilidad(ModelVenta venta, boolean elegible, int diasTranscurridos) {
            this.venta = venta;
            this.elegible = elegible;
            this.diasTranscurridos = diasTranscurridos;
        }

        public ModelVenta getVenta() {
            return venta;
        }

        public boolean isElegible() {
            return elegible;
        }

        public int getDiasTranscurridos() {
            return diasTranscurridos;
        }
    }

    // ====================================================================
    // MÉTODOS ADICIONALES PARA INTEGRACIÓN CON DEVOLUCIONES
    // ====================================================================
    /**
     * Valida si un producto/variante puede ser devuelto según stock y estado
     * ÚTIL para validaciones en tiempo real
     *
     * @param idProducto        ID del producto
     * @param idVariante        ID de la variante (puede ser 0)
     * @param cantidadADevolver Cantidad que se pretende devolver
     * @return Resultado de la validación
     * @throws SQLException Si ocurre un error en la base de datos
     */
    public ValidacionDevolucionResultado validarDisponibilidadParaDevolucion(
            int idProducto, int idVariante, int cantidadADevolver) throws SQLException {

        try {
            if (idVariante > 0) {
                // Validar variante específica
                ServiceProductVariant serviceProductVariant = new ServiceProductVariant();
                ModelProductVariant variante = serviceProductVariant.getVariantById(idVariante, false);

                if (variante == null) {
                    return new ValidacionDevolucionResultado(false,
                            "La variante específica ya no existe en el sistema");
                }

                if (!variante.isAvailable()) {
                    return new ValidacionDevolucionResultado(false,
                            "La variante ya no está disponible en el sistema");
                }

                // Verificar que el producto padre también esté activo
                ModelProduct producto = serviceProduct.getProductById(idProducto);
                if (producto == null || !producto.isActive()) {
                    return new ValidacionDevolucionResultado(false,
                            "El producto ya no está activo en el sistema");
                }

                return new ValidacionDevolucionResultado(true,
                        String.format("Variante válida para devolución. Stock actual: %d pares, %d cajas",
                                variante.getStockPairs(), variante.getStockBoxes()));

            } else {
                // Validar producto base
                ModelProduct producto = serviceProduct.getProductById(idProducto);

                if (producto == null) {
                    return new ValidacionDevolucionResultado(false,
                            "El producto ya no existe en el sistema");
                }

                if (!producto.isActive()) {
                    return new ValidacionDevolucionResultado(false,
                            "El producto ya no está activo en el sistema");
                }

                return new ValidacionDevolucionResultado(true,
                        String.format("Producto válido para devolución. Stock actual: %d pares, %d cajas",
                                producto.getPairsStock(), producto.getBoxesStock()));
            }

        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Error validando disponibilidad para devolución", e);
            return new ValidacionDevolucionResultado(false,
                    "Error verificando disponibilidad: " + e.getMessage());
        }
    }

    /**
     * Obtiene información resumida de todos los productos en una venta ÚTIL
     * para formularios de selección de devolución
     *
     * @param idVenta ID de la venta
     * @return Lista de información resumida de productos
     * @throws SQLException Si ocurre un error en la base de datos
     */
    public List<ProductoVentaResumen> obtenerResumenProductosVenta(int idVenta) throws SQLException {
        List<ProductoVentaResumen> resumen = new ArrayList<>();
        Connection con = null;

        try {
            con = conexion.getInstance().createConnection();

            // CONSULTA CORREGIDA - Compatible con esquema real
            String sql = "SELECT vd.id_detalle, vd.id_producto, "
                    + "COALESCE(vd.id_variante, 0) as id_variante, "
                    + "vd.cantidad, vd.precio_unitario, "
                    + "COALESCE(vd.descuento, 0) as descuento, "
                    + "vd.subtotal, "
                    + "COALESCE(vd.tipo_venta, 'par') as tipo_venta, "
                    + "p.nombre as producto_nombre, "
                    + "COALESCE(p.codigo_modelo, '') as codigo_modelo, "
                    + "COALESCE(pv.sku, '') as sku, "
                    + "COALESCE(pv.ean, '') as ean, "
                    + "COALESCE(t.numero, p.talla, '') as talla, "
                    + "COALESCE(c.nombre, p.color, '') as color, "
                    + // CORREGIDO: Usar stock de producto_variantes o valores por defecto
                    "COALESCE((SELECT SUM(ib2.Stock_par) FROM inventario_bodega ib2 WHERE ib2.id_variante = pv.id_variante AND ib2.activo = 1), 0) as stock_pares, "
                    + "COALESCE((SELECT SUM(ib2.Stock_caja) FROM inventario_bodega ib2 WHERE ib2.id_variante = pv.id_variante AND ib2.activo = 1), 0) as stock_cajas, "
                    + // Verificar si tiene imagen
                    "CASE WHEN (pv.imagen IS NOT NULL AND LENGTH(pv.imagen) > 0) "
                    + "     THEN 1 ELSE 0 END as tiene_imagen "
                    + "FROM venta_detalles vd "
                    + "INNER JOIN productos p ON vd.id_producto = p.id_producto "
                    + "LEFT JOIN producto_variantes pv ON vd.id_variante = pv.id_variante "
                    + "LEFT JOIN tallas t ON pv.id_talla = t.id_talla "
                    + "LEFT JOIN colores c ON pv.id_color = c.id_color "
                    + "WHERE vd.id_venta = ? AND vd.activo = 1 "
                    + "ORDER BY vd.id_detalle";

            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1, idVenta);

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        // Crear objeto usando constructor con double para compatibilidad
                        ProductoVentaResumen item = new ProductoVentaResumen(
                                rs.getInt("id_detalle"),
                                rs.getInt("id_producto"),
                                rs.getInt("id_variante"),
                                rs.getString("producto_nombre"),
                                rs.getString("codigo_modelo"),
                                rs.getString("sku"),
                                rs.getString("ean"),
                                rs.getString("talla"),
                                rs.getString("color"),
                                rs.getInt("cantidad"),
                                rs.getDouble("precio_unitario"),
                                rs.getDouble("descuento"),
                                rs.getDouble("subtotal"),
                                rs.getString("tipo_venta"),
                                rs.getInt("stock_pares"),
                                rs.getInt("stock_cajas"),
                                rs.getBoolean("tiene_imagen"));

                        resumen.add(item);

                        // Log para debug
                        System.out.println("SUCCESS  Producto cargado: " + item.getNombreProducto()
                                + " - Cantidad: " + item.getCantidad()
                                + " - Stock: " + item.getStockPares() + " pares, "
                                + item.getStockCajas() + " cajas");
                    }
                }
            }

            System.out.println("SUCCESS  Total productos cargados: " + resumen.size());

        } catch (SQLException e) {
            System.err.println("ERROR  Error cargando productos para venta " + idVenta + ": " + e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            if (con != null) {
                try {
                    con.close();
                } catch (SQLException ex) {
                    System.err.println("Error cerrando conexión: " + ex.getMessage());
                }
            }
        }

        return resumen;
    }

    /**
     * Método alternativo más robusto que maneja productos sin variantes NUEVA
     * IMPLEMENTACIÓN: Para casos donde no todos los productos tienen variantes
     */
    public List<ProductoVentaResumen> obtenerResumenProductosVentaRobusto(int idVenta) throws SQLException {
        List<ProductoVentaResumen> resumen = new ArrayList<>();
        Connection con = null;

        try {
            con = conexion.getInstance().createConnection();

            // Primero verificar estructura de tablas
            if (!verificarEstructuraTablas(con)) {
                throw new SQLException("La estructura de tablas no es compatible");
            }

            // Consulta robusta que funciona con o sin variantes
            String sql = "SELECT vd.id_detalle, vd.id_producto, "
                    + "COALESCE(vd.id_variante, 0) as id_variante, "
                    + "vd.cantidad, vd.precio_unitario, "
                    + "COALESCE(vd.descuento, 0) as descuento, "
                    + "vd.subtotal, "
                    + "CASE "
                    + "    WHEN vd.tipo_venta IS NOT NULL THEN vd.tipo_venta "
                    + "    WHEN vd.tipo_venta_custom IS NOT NULL THEN vd.tipo_venta_custom "
                    + "    ELSE 'par' "
                    + "END as tipo_venta, "
                    + "p.nombre as producto_nombre, "
                    + "p.codigo_modelo, "
                    + "COALESCE(pv.sku, CONCAT('PROD-', p.id_producto)) as sku, "
                    + "COALESCE(pv.ean, '') as ean, "
                    + "COALESCE(t.numero, p.talla, 'N/A') as talla, "
                    + "COALESCE(c.nombre, p.color, 'N/A') as color, "
                    + "COALESCE((SELECT SUM(ib2.Stock_par) FROM inventario_bodega ib2 WHERE ib2.id_variante = pv.id_variante AND ib2.activo = 1), 0) as stock_pares, "
                    + "COALESCE((SELECT SUM(ib2.Stock_caja) FROM inventario_bodega ib2 WHERE ib2.id_variante = pv.id_variante AND ib2.activo = 1), 0) as stock_cajas, "
                    + "CASE WHEN (pv.imagen IS NOT NULL AND LENGTH(pv.imagen) > 0) "
                    + "     THEN 1 ELSE 0 END as tiene_imagen "
                    + "FROM venta_detalles vd "
                    + "INNER JOIN productos p ON vd.id_producto = p.id_producto "
                    + "LEFT JOIN producto_variantes pv ON vd.id_variante = pv.id_variante "
                    + "LEFT JOIN tallas t ON pv.id_talla = t.id_talla "
                    + "LEFT JOIN colores c ON pv.id_color = c.id_color "
                    + "WHERE vd.id_venta = ? AND COALESCE(vd.activo, 1) = 1 "
                    + "ORDER BY vd.id_detalle";

            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1, idVenta);

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        try {
                            ProductoVentaResumen item = new ProductoVentaResumen(
                                    rs.getInt("id_detalle"),
                                    rs.getInt("id_producto"),
                                    rs.getInt("id_variante"),
                                    rs.getString("producto_nombre"),
                                    rs.getString("codigo_modelo"),
                                    rs.getString("sku"),
                                    rs.getString("ean"),
                                    rs.getString("talla"),
                                    rs.getString("color"),
                                    rs.getInt("cantidad"),
                                    rs.getDouble("precio_unitario"),
                                    rs.getDouble("descuento"),
                                    rs.getDouble("subtotal"),
                                    rs.getString("tipo_venta"),
                                    rs.getInt("stock_pares"),
                                    rs.getInt("stock_cajas"),
                                    rs.getBoolean("tiene_imagen"));

                            resumen.add(item);

                            System.out.println("SUCCESS  Producto procesado: " + item.getNombreProducto());

                        } catch (Exception e) {
                            System.err.println("WARNING  Error procesando producto ID: " + rs.getInt("id_detalle")
                                    + " - " + e.getMessage());
                            // Continuar con el siguiente producto
                        }
                    }
                }
            }

        } catch (SQLException e) {
            System.err.println("ERROR  Error en obtenerResumenProductosVentaRobusto: " + e.getMessage());
            throw e;
        } finally {
            if (con != null) {
                try {
                    con.close();
                } catch (SQLException ex) {
                    System.err.println("Error cerrando conexión: " + ex.getMessage());
                }
            }
        }

        return resumen;
    }

    /**
     * Verifica que las tablas tengan la estructura esperada
     */
    private boolean verificarEstructuraTablas(Connection con) throws SQLException {
        String sql = "SELECT TABLE_NAME, COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS "
                + "WHERE TABLE_SCHEMA = DATABASE() "
                + "AND TABLE_NAME IN ('venta_detalles', 'productos', 'producto_variantes') "
                + "AND COLUMN_NAME IN ('stock_por_pares', 'stock_por_cajas', 'activo', 'tipo_venta')";

        Set<String> columnasEncontradas = new HashSet<>();

        try (PreparedStatement ps = con.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                String tabla = rs.getString("TABLE_NAME");
                String columna = rs.getString("COLUMN_NAME");
                columnasEncontradas.add(tabla + "." + columna);
            }
        }

        // Verificar columnas críticas
        boolean tieneStockVariantes = columnasEncontradas.contains("producto_variantes.stock_por_pares")
                && columnasEncontradas.contains("producto_variantes.stock_por_cajas");

        if (!tieneStockVariantes) {
            System.err.println("WARNING  Advertencia: No se encontraron columnas de stock en producto_variantes");
        }

        return true; // Continuar aunque no sea perfecto
    }

    /**
     * Método de fallback para casos donde la consulta principal falla
     * IMPLEMENTACIÓN BÁSICA: Solo información esencial
     */
    public List<ProductoVentaResumen> obtenerResumenProductosVentaBasico(int idVenta) throws SQLException {
        List<ProductoVentaResumen> resumen = new ArrayList<>();
        Connection con = null;

        try {
            con = conexion.getInstance().createConnection();

            // Consulta mínima que debería funcionar en cualquier esquema
            String sql = "SELECT vd.id_detalle, vd.id_producto, "
                    + "COALESCE(vd.id_variante, 0) as id_variante, "
                    + "vd.cantidad, vd.precio_unitario, "
                    + "COALESCE(vd.descuento, 0) as descuento, "
                    + "vd.subtotal, "
                    + "p.nombre as producto_nombre, "
                    + "p.codigo_modelo "
                    + "FROM venta_detalles vd "
                    + "INNER JOIN productos p ON vd.id_producto = p.id_producto "
                    + "WHERE vd.id_venta = ? "
                    + "ORDER BY vd.id_detalle";

            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1, idVenta);

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        ProductoVentaResumen item = new ProductoVentaResumen(
                                rs.getInt("id_detalle"),
                                rs.getInt("id_producto"),
                                rs.getInt("id_variante"),
                                rs.getString("producto_nombre"),
                                rs.getString("codigo_modelo"),
                                "", // sku
                                "", // ean
                                "", // talla
                                "", // color
                                rs.getInt("cantidad"),
                                rs.getDouble("precio_unitario"),
                                rs.getDouble("descuento"),
                                rs.getDouble("subtotal"),
                                "par", // tipo_venta por defecto
                                0, // stock_pares
                                0, // stock_cajas
                                false // tiene_imagen
                        );

                        resumen.add(item);
                    }
                }
            }

            System.out.println("SUCCESS  Productos cargados (modo básico): " + resumen.size());

        } catch (SQLException e) {
            System.err.println("ERROR  Error en modo básico: " + e.getMessage());
            throw e;
        } finally {
            if (con != null) {
                try {
                    con.close();
                } catch (SQLException ex) {
                    System.err.println("Error cerrando conexión: " + ex.getMessage());
                }
            }
        }

        return resumen;
    }

    /**
     * Busca productos por código de barras o EAN específicamente para
     * devoluciones MEJORADO: Incluye validación de elegibilidad para devolución
     *
     * @param codigo Código de barras o EAN
     * @return Información del producto con estado de devolución
     * @throws SQLException Si ocurre un error en la base de datos
     */
    public ProductoDevolucionInfo buscarProductoParaDevolucionPorCodigo(String codigo) throws SQLException {
        // Primero buscar el producto usando el servicio existente
        ModelProduct producto = serviceProduct.getProductByBarcode(codigo);

        if (producto == null) {
            return null;
        }

        // Obtener información específica de la variante si existe
        ModelProductVariant variante = null;
        if (producto.getVariants() != null && !producto.getVariants().isEmpty()) {
            variante = producto.getVariants().get(0); // Primera variante encontrada
        }

        // Crear información específica para devolución
        return new ProductoDevolucionInfo(
                producto.getProductId(),
                variante != null ? variante.getVariantId() : 0,
                producto.getName(),
                producto.getModelCode(),
                variante != null ? variante.getSku() : null,
                variante != null ? variante.getEan() : null,
                variante != null ? variante.getSizeName() : producto.getSize(),
                variante != null ? variante.getColorName() : producto.getColor(),
                variante != null ? variante.getStockPairs() : producto.getPairsStock(),
                variante != null ? variante.getStockBoxes() : producto.getBoxesStock(),
                variante != null ? variante.getSalePrice() : producto.getSalePrice(),
                producto.isActive() && (variante == null || variante.isAvailable()),
                variante != null ? variante.hasImage()
                        : (producto.getProfile() != null && producto.getProfile().getImageBytes() != null));
    }

    // ====================================================================
    // CLASES AUXILIARES PARA DEVOLUCIONES
    // ====================================================================
    /**
     * Resultado de validación para devoluciones
     */
    public static class ValidacionDevolucionResultado {

        private final boolean valido;
        private final String mensaje;

        public ValidacionDevolucionResultado(boolean valido, String mensaje) {
            this.valido = valido;
            this.mensaje = mensaje;
        }

        public boolean isValido() {
            return valido;
        }

        public String getMensaje() {
            return mensaje;
        }
    }

    /**
     * Información resumida de producto en venta para devoluciones
     */
    /**
     * Información específica de producto para devoluciones
     */
    public static class ProductoDevolucionInfo {

        private final int idProducto;
        private final int idVariante;
        private final String nombre;
        private final String codigoModelo;
        private final String sku;
        private final String ean;
        private final String talla;
        private final String color;
        private final int stockPares;
        private final int stockCajas;
        private final double precio;
        private final boolean disponible;
        private final boolean tieneImagen;

        public ProductoDevolucionInfo(int idProducto, int idVariante, String nombre, String codigoModelo,
                String sku, String ean, String talla, String color, int stockPares,
                int stockCajas, double precio, boolean disponible, boolean tieneImagen) {
            this.idProducto = idProducto;
            this.idVariante = idVariante;
            this.nombre = nombre;
            this.codigoModelo = codigoModelo;
            this.sku = sku;
            this.ean = ean;
            this.talla = talla;
            this.color = color;
            this.stockPares = stockPares;
            this.stockCajas = stockCajas;
            this.precio = precio;
            this.disponible = disponible;
            this.tieneImagen = tieneImagen;
        }

        // Getters
        public int getIdProducto() {
            return idProducto;
        }

        public int getIdVariante() {
            return idVariante;
        }

        public String getNombre() {
            return nombre;
        }

        public String getCodigoModelo() {
            return codigoModelo;
        }

        public String getSku() {
            return sku;
        }

        public String getEan() {
            return ean;
        }

        public String getTalla() {
            return talla;
        }

        public String getColor() {
            return color;
        }

        public int getStockPares() {
            return stockPares;
        }

        public int getStockCajas() {
            return stockCajas;
        }

        public double getPrecio() {
            return precio;
        }

        public boolean isDisponible() {
            return disponible;
        }

        public boolean isTieneImagen() {
            return tieneImagen;
        }

        public int getTotalPares() {
            return stockPares + (stockCajas * 24);
        }

        public String getDescripcionCompleta() {
            StringBuilder desc = new StringBuilder(nombre);
            if (talla != null) {
                desc.append(" - Talla: ").append(talla);
            }
            if (color != null) {
                desc.append(" - Color: ").append(color);
            }
            return desc.toString();
        }

        public String getCodigoIdentificacion() {
            return sku != null ? sku : (ean != null ? ean : codigoModelo);
        }

        /**
         * Obtiene una variante de producto por su ID NUEVO: Método para obtener
         * información completa de una variante
         *
         * @param idVariante ID de la variante a buscar
         * @return ModelProductVariant con la información de la variante
         * @throws SQLException Si ocurre un error en la base de datos
         */
        public ModelProductVariant obtenerVariantePorId(int idVariante) throws SQLException {
            Connection con = null;
            ModelProductVariant variante = null;

            try {
                con = conexion.getInstance().createConnection();

                String sql = "SELECT pv.*, "
                        + "t.id_talla, t.numero as talla_numero, t.sistema as talla_sistema, "
                        + "c.id_color, c.nombre as color_nombre, c.codigo_hex as color_hex, "
                        + "p.id_producto, p.nombre as producto_nombre, p.codigo_modelo "
                        + "FROM producto_variantes pv "
                        + "LEFT JOIN tallas t ON pv.id_talla = t.id_talla "
                        + "LEFT JOIN colores c ON pv.id_color = c.id_color "
                        + "JOIN productos p ON pv.id_producto = p.id_producto "
                        + "WHERE pv.id_variante = ? AND pv.activo = 1";

                try (PreparedStatement ps = con.prepareStatement(sql)) {
                    ps.setInt(1, idVariante);

                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            variante = crearModelProductVariantCompleto(rs);

                            // También cargar información básica del producto padre
                            ModelProduct producto = new ModelProduct();
                            producto.setProductId(rs.getInt("id_producto"));
                            producto.setName(rs.getString("producto_nombre"));
                            producto.setModelCode(rs.getString("codigo_modelo"));

                            // NO usar setProduct() si no existe - en su lugar, establecer los campos
                            // directamente si es necesario
                            // variante.setProduct(producto); // ESTA LÍNCA CAUSA EL ERROR
                            System.out.println("SUCCESS  Variante encontrada - ID: " + idVariante
                                    + " - SKU: " + variante.getSku()
                                    + " - Producto: " + producto.getName());
                        }
                    }
                }

            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Error al obtener variante por ID: " + idVariante, e);
                throw new SQLException("Error al obtener la variante: " + e.getMessage(), e);
            } finally {
                if (con != null) {
                    try {
                        con.close();
                    } catch (SQLException ex) {
                        LOGGER.log(Level.WARNING, "Error al cerrar conexión", ex);
                    }
                }
            }

            return variante;
        }

        /**
         * Crea un ModelProductVariant completo desde un ResultSet (Método ya
         * existente pero ahora público para reutilización)
         *
         * @param rs ResultSet con datos de la variante
         * @return ModelProductVariant completo
         * @throws SQLException Si ocurre un error accediendo a los datos
         */
        public ModelProductVariant crearModelProductVariantCompleto(ResultSet rs) throws SQLException {
            ModelProductVariant variante = new ModelProductVariant();

            // ====================================================================
            // DATOS BÁSICOS DE LA VARIANTE
            // ====================================================================
            variante.setVariantId(rs.getInt("id_variante"));
            variante.setProductId(rs.getInt("id_producto"));
            variante.setSku(rs.getString("sku"));
            variante.setEan(rs.getString("ean"));
            variante.setBarcode(rs.getString("codigo_barras"));
            variante.setAvailable(rs.getBoolean("disponible"));

            // ====================================================================
            // STOCKS
            // ====================================================================
            variante.setStockPairs(rs.getInt("stock_por_pares"));
            variante.setStockBoxes(rs.getInt("stock_por_cajas"));

            if (rs.getObject("stock_minimo_variante") != null) {
                variante.setMinStock(rs.getInt("stock_minimo_variante"));
            }

            // ====================================================================
            // PRECIOS ESPECÍFICOS DE LA VARIANTE
            // ====================================================================
            if (rs.getObject("precio_compra") != null) {
                variante.setPurchasePrice(rs.getDouble("precio_compra"));
            }

            if (rs.getObject("precio_venta") != null) {
                variante.setSalePrice(rs.getDouble("precio_venta"));
            }

            // ====================================================================
            // INFORMACIÓN DE TALLA
            // ====================================================================
            if (rs.getObject("id_talla") != null) {
                variante.setSizeId(rs.getInt("id_talla"));
                String tallaNumero = rs.getString("talla_numero");
                String tallaSistema = rs.getString("talla_sistema");

                if (tallaNumero != null) {
                    String nombreCompleto = tallaNumero;
                    if (tallaSistema != null && !tallaSistema.isEmpty()) {
                        nombreCompleto += " " + tallaSistema;
                    }
                    variante.setSizeName(nombreCompleto);
                    variante.setSizeSystem(tallaSistema);
                }
            }

            // ====================================================================
            // INFORMACIÓN DE COLOR
            // ====================================================================
            if (rs.getObject("id_color") != null) {
                variante.setColorId(rs.getInt("id_color"));
                variante.setColorName(rs.getString("color_nombre"));
                variante.setColorHex(rs.getString("color_hex"));
            }

            // ====================================================================
            // IMAGEN DE LA VARIANTE
            // ====================================================================
            byte[] imagenBytes = rs.getBytes("imagen");
            if (imagenBytes != null && imagenBytes.length > 0) {
                variante.setImageBytes(imagenBytes);
            }

            return variante;
        }
    }

    public static class ProductoVentaResumen {

        // ====================================================================
        // ATRIBUTOS PRINCIPALES
        // ====================================================================
        private final int idDetalle;
        private final int idProducto;
        private final int idVariante;
        private final String nombreProducto;
        private final String codigoModelo;
        private final String sku;
        private final String ean;
        private final String talla;
        private final String color;
        private final int cantidad;
        private final BigDecimal precioUnitario;
        private final BigDecimal descuento;
        private final BigDecimal subtotal;
        private final String tipoVenta;
        private final int stockPares;
        private final int stockCajas;
        private final boolean tieneImagen;

        // Atributos para devoluciones (mutables)
        private int cantidadDevolver;
        private String motivoDevolucion;

        public ProductoVentaResumen(int idDetalle, int idProducto, int idVariante,
                String nombreProducto, String codigoModelo, String sku, String ean,
                String talla, String color, int cantidad, BigDecimal precioUnitario,
                BigDecimal descuento, BigDecimal subtotal, String tipoVenta,
                int stockPares, int stockCajas, boolean tieneImagen) {

            this.idDetalle = idDetalle;
            this.idProducto = idProducto;
            this.idVariante = idVariante;
            this.nombreProducto = nombreProducto != null ? nombreProducto : "Sin nombre";
            this.codigoModelo = codigoModelo;
            this.sku = sku;
            this.ean = ean;
            this.talla = talla;
            this.color = color;
            this.cantidad = cantidad;
            this.precioUnitario = precioUnitario != null ? precioUnitario : BigDecimal.ZERO;
            this.descuento = descuento != null ? descuento : BigDecimal.ZERO;
            this.subtotal = subtotal != null ? subtotal : BigDecimal.ZERO;
            this.tipoVenta = tipoVenta != null ? tipoVenta : "par";
            this.stockPares = stockPares;
            this.stockCajas = stockCajas;
            this.tieneImagen = tieneImagen;

            // Inicializar valores de devolución
            this.cantidadDevolver = 0;
            this.motivoDevolucion = "";
        }

        /**
         * Constructor alternativo con double para compatibilidad hacia atrás
         * Convierte automáticamente a BigDecimal
         */
        public ProductoVentaResumen(int idDetalle, int idProducto, int idVariante,
                String nombreProducto, String codigoModelo, String sku, String ean,
                String talla, String color, int cantidad, double precioUnitario,
                double descuento, double subtotal, String tipoVenta,
                int stockPares, int stockCajas, boolean tieneImagen) {

            this(idDetalle, idProducto, idVariante, nombreProducto, codigoModelo,
                    sku, ean, talla, color, cantidad,
                    BigDecimal.valueOf(precioUnitario),
                    BigDecimal.valueOf(descuento),
                    BigDecimal.valueOf(subtotal),
                    tipoVenta, stockPares, stockCajas, tieneImagen);
        }

        // ====================================================================
        // GETTERS PRINCIPALES
        // ====================================================================
        public int getIdDetalle() {
            return idDetalle;
        }

        public int getIdProducto() {
            return idProducto;
        }

        public int getIdVariante() {
            return idVariante;
        }

        public String getNombreProducto() {
            return nombreProducto;
        }

        public String getCodigoModelo() {
            return codigoModelo;
        }

        public String getSku() {
            return sku;
        }

        public String getEan() {
            return ean;
        }

        public String getTalla() {
            return talla;
        }

        public String getColor() {
            return color;
        }

        public int getCantidad() {
            return cantidad;
        }

        public BigDecimal getPrecioUnitario() {
            return precioUnitario;
        }

        public BigDecimal getDescuento() {
            return descuento;
        }

        public BigDecimal getSubtotal() {
            return subtotal;
        }

        public String getTipoVenta() {
            return tipoVenta;
        }

        public int getStockPares() {
            return stockPares;
        }

        public int getStockCajas() {
            return stockCajas;
        }

        public boolean isTieneImagen() {
            return tieneImagen;
        }

        // ====================================================================
        // GETTERS/SETTERS PARA DEVOLUCIONES
        // ====================================================================
        public int getCantidadDevolver() {
            return cantidadDevolver;
        }

        public void setCantidadDevolver(int cantidadDevolver) {
            this.cantidadDevolver = Math.max(0, Math.min(cantidadDevolver, cantidad));
        }

        public String getMotivoDevolucion() {
            return motivoDevolucion;
        }

        public void setMotivoDevolucion(String motivoDevolucion) {
            this.motivoDevolucion = motivoDevolucion != null ? motivoDevolucion : "";
        }

        // ====================================================================
        // MÉTODOS DE COMPATIBILIDAD (DOUBLE)
        // ====================================================================
        /**
         * Obtiene precio unitario como double para compatibilidad
         */
        public double getPrecioUnitarioDouble() {
            return precioUnitario.doubleValue();
        }

        /**
         * Obtiene descuento como double para compatibilidad
         */
        public double getDescuentoDouble() {
            return descuento.doubleValue();
        }

        /**
         * Obtiene subtotal como double para compatibilidad
         */
        public double getSubtotalDouble() {
            return subtotal.doubleValue();
        }

        /**
         * Obtiene subtotal de devolución como double
         */
        public double getSubtotalDevolucionDouble() {
            return getSubtotalDevolucion().doubleValue();
        }

        // ====================================================================
        // MÉTODOS DE CÁLCULO
        // ====================================================================
        /**
         * Calcula el subtotal de devolución basado en cantidad a devolver
         */
        public BigDecimal getSubtotalDevolucion() {
            if (cantidadDevolver <= 0) {
                return BigDecimal.ZERO;
            }

            // Calcular descuento proporcional por unidad
            BigDecimal descuentoPorUnidad = cantidad > 0
                    ? descuento.divide(BigDecimal.valueOf(cantidad), 4, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            // Calcular precio efectivo por unidad (precio - descuento)
            BigDecimal precioEfectivo = precioUnitario.subtract(descuentoPorUnidad);

            // Calcular subtotal de devolución
            return precioEfectivo.multiply(BigDecimal.valueOf(cantidadDevolver))
                    .setScale(2, RoundingMode.HALF_UP);
        }

        /**
         * Calcula total de pares disponibles (pares + cajas * 24)
         */
        public int getTotalPares() {
            return stockPares + (stockCajas * 24);
        }

        // ====================================================================
        // MÉTODOS DE VALIDACIÓN
        // ====================================================================
        /**
         * Verifica si el producto puede ser devuelto según configuración actual
         */
        public boolean puedeDevolverse() {
            return cantidadDevolver > 0
                    && cantidadDevolver <= cantidad
                    && motivoDevolucion != null
                    && !motivoDevolucion.trim().isEmpty();
        }

        /**
         * Verifica si tiene variante específica
         */
        public boolean tieneVariante() {
            return idVariante > 0;
        }

        /**
         * Verifica si tiene stock disponible
         */
        public boolean tieneStock() {
            return getTotalPares() > 0;
        }

        // ====================================================================
        // MÉTODOS DE PRESENTACIÓN
        // ====================================================================
        /**
         * Obtiene descripción completa del producto incluyendo variante
         */
        public String getDescripcionCompleta() {
            StringBuilder desc = new StringBuilder(nombreProducto);

            if (talla != null && !talla.trim().isEmpty()) {
                desc.append(" - Talla: ").append(talla);
            }

            if (color != null && !color.trim().isEmpty()) {
                desc.append(" - Color: ").append(color);
            }

            if (sku != null && !sku.trim().isEmpty()) {
                desc.append(" (").append(sku).append(")");
            }

            return desc.toString();
        }

        /**
         * Obtiene información de la variante formateada
         */
        public String getInfoVariante() {
            if (!tieneVariante()) {
                return "Sin variante";
            }

            StringBuilder info = new StringBuilder();

            if (talla != null && !talla.trim().isEmpty()) {
                info.append("Talla: ").append(talla);
            }

            if (color != null && !color.trim().isEmpty()) {
                if (info.length() > 0) {
                    info.append(", ");
                }
                info.append("Color: ").append(color);
            }

            if (info.length() == 0) {
                if (sku != null && !sku.trim().isEmpty()) {
                    info.append("SKU: ").append(sku);
                } else {
                    info.append("Variante ID: ").append(idVariante);
                }
            }

            return info.toString();
        }

        /**
         * Obtiene código identificador principal (SKU, EAN o código modelo)
         */
        public String getCodigoIdentificador() {
            if (sku != null && !sku.trim().isEmpty()) {
                return sku;
            }
            if (ean != null && !ean.trim().isEmpty()) {
                return ean;
            }
            if (codigoModelo != null && !codigoModelo.trim().isEmpty()) {
                return codigoModelo;
            }
            return "PROD-" + idProducto;
        }

        // ====================================================================
        // MÉTODOS DE COMPARACIÓN Y UTILIDAD
        // ====================================================================
        @Override
        public String toString() {
            return String.format("ProductoVentaResumen{id=%d, producto='%s', cantidad=%d, aDevolver=%d}",
                    idDetalle, nombreProducto, cantidad, cantidadDevolver);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            ProductoVentaResumen that = (ProductoVentaResumen) obj;
            return idDetalle == that.idDetalle;
        }

        @Override
        public int hashCode() {
            return Integer.hashCode(idDetalle);
        }

        // ====================================================================
        // MÉTODOS DE VALIDACIÓN DE DATOS
        // ====================================================================
        /**
         * Valida que los datos del producto sean consistentes
         */
        public boolean esValido() {
            return idDetalle > 0
                    && idProducto > 0
                    && nombreProducto != null
                    && !nombreProducto.trim().isEmpty()
                    && cantidad > 0
                    && precioUnitario != null
                    && precioUnitario.compareTo(BigDecimal.ZERO) >= 0;
        }

        /**
         * Obtiene mensaje de validación si hay errores
         */
        public String getMensajeValidacion() {
            if (!esValido()) {
                StringBuilder errores = new StringBuilder("Errores en el producto:\n");

                if (idDetalle <= 0) {
                    errores.append("- ID detalle inválido\n");
                }
                if (idProducto <= 0) {
                    errores.append("- ID producto inválido\n");
                }
                if (nombreProducto == null || nombreProducto.trim().isEmpty()) {
                    errores.append("- Nombre de producto faltante\n");
                }
                if (cantidad <= 0) {
                    errores.append("- Cantidad inválida\n");
                }
                if (precioUnitario == null || precioUnitario.compareTo(BigDecimal.ZERO) < 0) {
                    errores.append("- Precio inválido\n");
                }

                return errores.toString();
            }
            return "Producto válido";
        }
    }
}
