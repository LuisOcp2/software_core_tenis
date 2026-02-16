package raven.clases.comercial;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import raven.controlador.comercial.ModelDevolucion;
import raven.controlador.comercial.ModelDetalleDevolucion;
import raven.controlador.principal.ModelVenta;
import raven.controlador.principal.ModelDetalleVenta;
import raven.controlador.principal.conexion;
import raven.controlador.comercial.ModelCliente;
import raven.controlador.admin.ModelUser;
import raven.controlador.comercial.ModelNotaCredito;

/**
 * Servicio completo para el manejo de devoluciones - IMPLEMENTACIÓN FINAL
 * Incluye procesamiento completo de devoluciones y notas de crédito
 *
 * Notas: - Todas las consultas SQL usan String clásico (no text blocks) para
 * compatibilidad. - Manejo de transacción: commit al final, rollback en
 * excepciones. - Validaciones preventivas antes de tocar DB.
 *
 * Autor: Sistema Versión: 1.2
 */
public class ServiceDevolucion {

    private static final String PREFIJO_NUMERO_DEVOLUCION = "DEV";
    private static final int DIAS_LIMITE_DEVOLUCION = 30;
    private static final BigDecimal MONTO_AUTORIZACION_REQUERIDA = new BigDecimal("500000.00");
    private static final BigDecimal PORCENTAJE_IVA = new BigDecimal("0.19");

    /**
     * Clase para productos en devolución
     */
    public static class ProductoDevolucion {

        private final int idDetalleVenta;
        private final int idProducto;
        private final int idVariante;
        private final int cantidadOriginal;
        private final int cantidadDevolver;
        private final BigDecimal precioUnitario;
        private final BigDecimal descuento;
        private final String observaciones;

        public ProductoDevolucion(int idDetalleVenta, int idProducto, int idVariante,
                int cantidadOriginal, int cantidadDevolver,
                BigDecimal precioUnitario, BigDecimal descuento, String observaciones) {
            this.idDetalleVenta = idDetalleVenta;
            this.idProducto = idProducto;
            this.idVariante = idVariante;
            this.cantidadOriginal = cantidadOriginal;
            this.cantidadDevolver = cantidadDevolver;
            this.precioUnitario = precioUnitario;
            this.descuento = descuento != null ? descuento : BigDecimal.ZERO;
            this.observaciones = observaciones;
        }

        // Getters
        public int getIdDetalleVenta() {
            return idDetalleVenta;
        }

        public int getIdProducto() {
            return idProducto;
        }

        public int getIdVariante() {
            return idVariante;
        }

        public int getCantidadOriginal() {
            return cantidadOriginal;
        }

        public int getCantidadDevolver() {
            return cantidadDevolver;
        }

        public BigDecimal getPrecioUnitario() {
            return precioUnitario;
        }

        public BigDecimal getDescuento() {
            return descuento;
        }

        public String getObservaciones() {
            return observaciones;
        }

        /**
         * Calcula subtotal proporcional considerando descuento proporcional.
         */
        public BigDecimal calcularSubtotal() {
            BigDecimal subtotal = precioUnitario.multiply(new BigDecimal(cantidadDevolver));
            if (cantidadOriginal > 0) {
                BigDecimal descuentoProporcional = descuento.multiply(new BigDecimal(cantidadDevolver))
                        .divide(new BigDecimal(cantidadOriginal), 2, BigDecimal.ROUND_HALF_UP);
                return subtotal.subtract(descuentoProporcional);
            }
            return subtotal;
        }

        /**
         * Validaciones simples por producto.
         */
        public void validar() {
            if (cantidadDevolver <= 0) {
                throw new IllegalArgumentException("Cantidad a devolver debe ser mayor a 0");
            }
            if (cantidadDevolver > cantidadOriginal) {
                throw new IllegalArgumentException(
                        String.format("No se puede devolver más de lo vendido. Vendido: %d, A devolver: %d",
                                cantidadOriginal, cantidadDevolver));
            }
            if (precioUnitario == null || precioUnitario.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Precio unitario debe ser mayor a 0");
            }
        }
    }

    /**
     * Procesa una devolución completa MODIFICADO: NO actualiza inventario aquí,
     * solo al autorizar
     */
    /**
     * Procesa una devolución completa MODIFICADO: NO actualiza inventario aquí,
     * solo al autorizar
     */
    public ModelDevolucion procesarDevolucion(JComponent parentComponent,
            ModelVenta venta, List<ProductoDevolucion> productos,
            String motivo, int idUsuario) {

        Connection conn = null;

        try {
            // VALIDACIÓN CRÍTICA: Verificar devoluciones previas
            if (!validarNoExistanDevolucionesPrevias(venta.getIdVenta())) {
                JOptionPane.showMessageDialog(parentComponent,
                        "Esta venta ya tiene una devolución procesada o en proceso.\n"
                                + "No es posible generar múltiples devoluciones para la misma venta.\n\n"
                                + "Verifique el historial de devoluciones si necesita más información.",
                        "Devolución Duplicada",
                        JOptionPane.WARNING_MESSAGE);
                return null;
            }

            validarDevolucionCompleta(venta, productos);

            conn = conexion.getInstance().getConnection();
            conn.setAutoCommit(false);

            // 1. Crear devolución principal (estado: PENDIENTE)
            ModelDevolucion devolucion = crearDevolucionPrincipal(conn, venta, motivo, idUsuario);

            // 2. Procesar detalles
            List<ModelDetalleDevolucion> detalles = procesarDetallesProductos(conn, devolucion, productos);

            // 3. Calcular totales y actualizar la devolución
            calcularTotalesDevolucion(conn, devolucion, detalles);

            // 5. NUEVO: Auto-aprobación si no requiere autorización
            if (!devolucion.isRequiereAutorizacion()) {
                System.out.println(
                        "INFO  Devolución " + devolucion.getNumeroDevolucion() + " califica para AUTO-APROBACIÓN");

                // Actualizar estado a APROBADA
                actualizarEstadoDevolucion(conn, devolucion.getIdDevolucion(), "aprobada", idUsuario,
                        "Auto-aprobada por sistema (Monto < $" + MONTO_AUTORIZACION_REQUERIDA + ")");
                devolucion.setEstado(ModelDevolucion.EstadoDevolucion.APROBADA);

                // Actualizar inventarios
                actualizarInventariosEnAutorizacion(conn, detalles);

                // Generar nota de crédito
                String numeroNC = generarNotaCredito(conn, devolucion);
                devolucion.setNumeroNotaCredito(numeroNC); // Asumiendo que existe el setter, si no, se manejará después

                // Registrar movimientos
                registrarMovimientosInventario(conn, detalles, devolucion);

                // Marcar venta
                marcarVentaConDevolucion(conn, venta.getIdVenta(), devolucion.getTotalDevolucion());

                System.out.println("SUCCESS  Devolución auto-aprobada y procesada completamente");
            } else {
                // 4. Marcar venta con devolución pendiente (si no fue auto-aprobada)
                marcarVentaConDevolucionPendiente(conn, venta.getIdVenta());
            }

            conn.commit();

            return devolucion;

        } catch (Exception e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    System.err.println("Error en rollback: " + ex.getMessage());
                }
            }

            JOptionPane.showMessageDialog(parentComponent,
                    "Error procesando devolución: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            return null;

        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    System.err.println("Error cerrando conexión: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Marca venta con devolución PENDIENTE (no completamente devuelta)
     */
    private void marcarVentaConDevolucionPendiente(Connection conn, int idVenta) throws SQLException {
        String sql = "UPDATE ventas SET "
                + "observaciones = CONCAT(COALESCE(observaciones, ''), ' - DEVOLUCIÓN PENDIENTE AUTORIZACIÓN') "
                + "WHERE id_venta = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idVenta);
            stmt.executeUpdate();
        }
    }

    /**
     * Autoriza una devolución y ACTUALIZA EL INVENTARIO ESTE ES EL MOMENTO
     * donde se hace el reingreso al inventario
     *
     * @param idDevolucion      ID de la devolución a autorizar
     * @param idUsuarioAutoriza ID del usuario que autoriza
     * @param aprobada          true si se aprueba, false si se rechaza
     * @param observaciones     Observaciones de la autorización
     * @return true si se autorizó exitosamente
     */
    public boolean autorizarDevolucion(int idDevolucion, int idUsuarioAutoriza,
            boolean aprobada, String observaciones) {
        Connection conn = null;

        try {
            conn = conexion.getInstance().getConnection();
            conn.setAutoCommit(false);

            System.out.println(" Iniciando autorización de devolución " + idDevolucion);

            // 1. Verificar que la devolución existe y está pendiente
            ModelDevolucion devolucion = obtenerDevolucionPorId(conn, idDevolucion);

            if (devolucion == null) {
                throw new IllegalStateException("Devolución no encontrada");
            }

            if (!"pendiente".equals(devolucion.getEstado().getValor())) {
                throw new IllegalStateException("Solo se pueden autorizar devoluciones PENDIENTES. "
                        + "Estado actual: " + devolucion.getEstado().getDescripcion());
            }

            // 2. Obtener detalles de la devolución
            List<ModelDetalleDevolucion> detalles = obtenerDetallesDevolucion(conn, idDevolucion);

            if (detalles.isEmpty()) {
                throw new IllegalStateException("La devolución no tiene productos asociados");
            }

            String nuevoEstado = aprobada ? "aprobada" : "rechazada";

            // 3. Actualizar estado de la devolución
            actualizarEstadoDevolucion(conn, idDevolucion, nuevoEstado,
                    idUsuarioAutoriza, observaciones);

            if (aprobada) {
                System.out.println("SUCCESS  Devolución APROBADA - Procesando inventario y nota de crédito...");

                // 4. AHORA SÍ: Actualizar inventarios (reingreso)
                actualizarInventariosEnAutorizacion(conn, detalles);

                // 5. Generar nota de crédito
                generarNotaCredito(conn, devolucion);

                // 6. Registrar movimientos de inventario
                registrarMovimientosInventario(conn, detalles, devolucion);

                // 7. Marcar venta como devuelta si aplica
                marcarVentaConDevolucion(conn, devolucion.getIdVenta(), devolucion.getTotalDevolucion());

                System.out.println("SUCCESS  Inventario actualizado, nota de crédito generada");

            } else {
                System.out.println("ERROR  Devolución RECHAZADA - No se modifica inventario");
            }

            conn.commit();

            System.out.println(String.format("SUCCESS  Devolución %s exitosamente - ID: %d",
                    aprobada ? "APROBADA" : "RECHAZADA", idDevolucion));

            return true;

        } catch (Exception e) {
            if (conn != null) {
                try {
                    conn.rollback();
                    System.err.println("WARNING  Rollback ejecutado");
                } catch (SQLException ex) {
                    System.err.println("ERROR  Error en rollback: " + ex.getMessage());
                }
            }

            System.err.println("ERROR  Error autorizando devolución: " + e.getMessage());
            e.printStackTrace();
            return false;

        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    System.err.println("Error cerrando conexión: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Obtiene una devolución por ID
     */
    private ModelDevolucion obtenerDevolucionPorId(Connection conn, int idDevolucion) throws SQLException {
        String sql = "SELECT * FROM devoluciones WHERE id_devolucion = ? AND activa = 1";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idDevolucion);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapearDevolucionCompleta(rs);
                }
            }
        }

        return null;
    }

    /**
     * Obtiene los detalles de una devolución
     */
    private List<ModelDetalleDevolucion> obtenerDetallesDevolucion(Connection conn, int idDevolucion)
            throws SQLException {
        List<ModelDetalleDevolucion> detalles = new ArrayList<>();

        String sql = "SELECT * FROM devolucion_detalles WHERE id_devolucion = ? AND activo = 1";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idDevolucion);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    ModelDetalleDevolucion detalle = new ModelDetalleDevolucion();
                    detalle.setIdDetalleDevolucion(rs.getInt("id_detalle_devolucion"));
                    detalle.setIdDevolucion(rs.getInt("id_devolucion"));
                    detalle.setIdDetalleVenta(rs.getInt("id_detalle_venta"));
                    detalle.setIdProducto(rs.getInt("id_producto"));
                    detalle.setIdVariante(rs.getInt("id_variante"));
                    detalle.setCantidadDevuelta(rs.getInt("cantidad_devuelta"));
                    detalle.setCondicionProducto(
                            ModelDetalleDevolucion.CondicionProducto.fromString(rs.getString("condicion_producto")));
                    detalle.setAccionProducto(
                            ModelDetalleDevolucion.AccionProducto.fromString(rs.getString("accion_producto")));

                    detalles.add(detalle);
                }
            }
        }

        return detalles;
    }

    /**
     * Obtiene los detalles de una devolución INCLUYENDO INFORMACIÓN DE PRODUCTO
     * Para uso en UI
     */
    public List<ModelDetalleDevolucion> getDetallesConProductos(int idDevolucion) throws SQLException {
        List<ModelDetalleDevolucion> detalles = new ArrayList<>();

        String sql = "SELECT dd.*, p.nombre as nombre_producto, " +
                "CONCAT(t.numero, ' ', c.nombre) as nombre_variante " +
                "FROM devolucion_detalles dd " +
                "INNER JOIN productos p ON dd.id_producto = p.id_producto " +
                "LEFT JOIN producto_variantes pv ON dd.id_variante = pv.id_variante " +
                "LEFT JOIN tallas t ON pv.id_talla = t.id_talla " +
                "LEFT JOIN colores c ON pv.id_color = c.id_color " +
                "WHERE dd.id_devolucion = ? AND dd.activo = 1";

        try (Connection conn = conexion.getInstance().getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idDevolucion);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    ModelDetalleDevolucion detalle = new ModelDetalleDevolucion();
                    detalle.setIdDetalleDevolucion(rs.getInt("id_detalle_devolucion"));
                    detalle.setIdDevolucion(rs.getInt("id_devolucion"));
                    detalle.setIdDetalleVenta(rs.getInt("id_detalle_venta"));
                    detalle.setIdProducto(rs.getInt("id_producto"));
                    detalle.setIdVariante(rs.getInt("id_variante"));
                    detalle.setCantidadDevuelta(rs.getInt("cantidad_devuelta"));
                    detalle.setCondicionProducto(
                            ModelDetalleDevolucion.CondicionProducto.fromString(rs.getString("condicion_producto")));
                    detalle.setAccionProducto(
                            ModelDetalleDevolucion.AccionProducto.fromString(rs.getString("accion_producto")));
                    detalle.setObservacionesDetalle(rs.getString("observaciones_detalle"));
                    detalle.setSubtotalDevolucion(rs.getBigDecimal("subtotal_devolucion"));

                    // Campos extendidos
                    detalle.setNombreProducto(rs.getString("nombre_producto"));
                    String variante = rs.getString("nombre_variante");
                    detalle.setNombreVariante(variante != null ? variante : "N/A");

                    detalles.add(detalle);
                }
            }
        }
        return detalles;
    }

    /**
     * Actualiza el estado de la devolución
     */
    private void actualizarEstadoDevolucion(Connection conn, int idDevolucion, String nuevoEstado,
            int idUsuarioAutoriza, String observaciones) throws SQLException {
        String sql = "UPDATE devoluciones SET "
                + "estado = ?, "
                + "id_usuario_autoriza = ?, "
                + "fecha_autorizacion = NOW(), "
                + "observaciones_autorizacion = ? "
                + "WHERE id_devolucion = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, nuevoEstado);
            stmt.setInt(2, idUsuarioAutoriza);
            stmt.setString(3, observaciones);
            stmt.setInt(4, idDevolucion);

            int filasActualizadas = stmt.executeUpdate();

            if (filasActualizadas == 0) {
                throw new SQLException("No se pudo actualizar el estado de la devolución");
            }
        }
    }

    /**
     * ACTUALIZACIÓN DE INVENTARIOS - VERSIÓN PARA AUTORIZACIÓN Este método SÍ
     * actualiza el inventario cuando se aprueba
     */
    private void actualizarInventariosEnAutorizacion(Connection conn,
            List<ModelDetalleDevolucion> detalles)
            throws SQLException {

        // Primero, obtener el id_bodega de la venta original
        // La venta está asociada a una caja (id_caja), y la caja pertenece a una bodega
        Integer idBodega = null;
        if (!detalles.isEmpty()) {
            String sqlBodega = "SELECT c.id_bodega FROM venta_detalles vd "
                    + "JOIN ventas v ON vd.id_venta = v.id_venta "
                    + "JOIN cajas c ON v.id_caja = c.id_caja "
                    + "WHERE vd.id_detalle = ? LIMIT 1";
            try (PreparedStatement stmtBodega = conn.prepareStatement(sqlBodega)) {
                stmtBodega.setInt(1, detalles.get(0).getIdDetalleVenta());
                try (ResultSet rs = stmtBodega.executeQuery()) {
                    if (rs.next()) {
                        idBodega = rs.getInt("id_bodega");
                    }
                }
            }
        }

        // Si no se encontró bodega en la venta, usar la bodega del usuario actual
        if (idBodega == null) {
            idBodega = raven.clases.admin.UserSession.getInstance().getIdBodegaUsuario();
            System.out.println("WARNING  No se encontró bodega en la venta, usando bodega del usuario: " + idBodega);
        }

        String sqlInventario = "UPDATE inventario_bodega "
                + "SET Stock_par = Stock_par + ?, "
                + "    fecha_ultimo_movimiento = NOW() "
                + "WHERE id_variante = ? AND id_bodega = ?";

        String sqlProducto = "UPDATE productos "
                + "SET fecha_actualizacion = NOW() "
                + "WHERE id_producto = ?";

        try (PreparedStatement stmtInventario = conn.prepareStatement(sqlInventario);
                PreparedStatement stmtProducto = conn.prepareStatement(sqlProducto)) {

            for (ModelDetalleDevolucion detalle : detalles) {
                // Solo reingresar si el producto está en buen estado
                if (detalle.getCondicionProducto() == ModelDetalleDevolucion.CondicionProducto.NUEVO
                        || detalle.getCondicionProducto() == ModelDetalleDevolucion.CondicionProducto.USADO_BUENO) {

                    if (detalle.getIdVariante() > 0) {
                        // Actualizar stock de variante en la bodega correspondiente
                        stmtInventario.setInt(1, detalle.getCantidadDevuelta());
                        stmtInventario.setInt(2, detalle.getIdVariante());
                        stmtInventario.setInt(3, idBodega);
                        stmtInventario.addBatch();

                        System.out.println("SUCCESS  Reingresando " + detalle.getCantidadDevuelta()
                                + " unidades a variante " + detalle.getIdVariante()
                                + " en bodega " + idBodega);
                    }

                    // Actualizar timestamp del producto
                    stmtProducto.setInt(1, detalle.getIdProducto());
                    stmtProducto.addBatch();

                } else {
                    System.out.println("WARNING  Producto en mal estado NO reingresado - ID: "
                            + detalle.getIdProducto() + ", Condición: "
                            + detalle.getCondicionProducto().getDescripcion());
                }
            }

            stmtInventario.executeBatch();
            stmtProducto.executeBatch();

            System.out.println("SUCCESS  Inventarios actualizados exitosamente");
        }
    }

    private boolean validarNoExistanDevolucionesPrevias(int idVenta) throws SQLException {
        String sql = "SELECT COUNT(*) as total_devoluciones, "
                + "GROUP_CONCAT(DISTINCT estado) as estados, "
                + "GROUP_CONCAT(DISTINCT numero_devolucion) as numeros "
                + "FROM devoluciones "
                + "WHERE id_venta = ? AND activa = 1";

        try (Connection conn = conexion.getInstance().getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, idVenta);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    int totalDevoluciones = rs.getInt("total_devoluciones");

                    if (totalDevoluciones > 0) {
                        String estados = rs.getString("estados");
                        String numeros = rs.getString("numeros");

                        System.err.println(String.format(
                                "WARNING  Devolución duplicada detectada - Venta: %d, "
                                        + "Devoluciones existentes: %d, Estados: %s, Números: %s",
                                idVenta, totalDevoluciones, estados, numeros));

                        return false; // Ya existen devoluciones
                    }
                }
            }
        }

        return true; // No hay devoluciones previas
    }

    /**
     * Obtiene información detallada de devoluciones existentes para una venta
     * UTILIDAD: Proporciona contexto para diagnóstico y soporte
     *
     * @param idVenta ID de la venta
     * @return Información de devoluciones existentes
     * @throws SQLException Si hay error de acceso a datos
     */
    public static DevolucionExistente obtenerInformacionDevolucionExistente(int idVenta) throws SQLException {
        String sql = "SELECT d.numero_devolucion, d.estado, d.fecha_devolucion, "
                + "d.total_devolucion, d.tipo_devolucion, d.motivo, "
                + "u.nombre as usuario_procesa, "
                + "COUNT(dd.id_detalle_devolucion) as items_devueltos "
                + "FROM devoluciones d "
                + "LEFT JOIN usuarios u ON d.id_usuario_procesa = u.id_usuario "
                + "LEFT JOIN devolucion_detalles dd ON d.id_devolucion = dd.id_devolucion "
                + "WHERE d.id_venta = ? AND d.activa = 1 "
                + "GROUP BY d.id_devolucion "
                + "ORDER BY d.fecha_devolucion DESC "
                + "LIMIT 1";

        try (Connection conn = conexion.getInstance().getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, idVenta);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new DevolucionExistente(
                            rs.getString("numero_devolucion"),
                            rs.getString("estado"),
                            rs.getTimestamp("fecha_devolucion").toLocalDateTime(),
                            rs.getBigDecimal("total_devolucion"),
                            rs.getString("tipo_devolucion"),
                            rs.getString("motivo"),
                            rs.getString("usuario_procesa"),
                            rs.getInt("items_devueltos"));
                }
            }
        }

        return null;
    }

    /**
     * VALIDACIONES CRÍTICAS
     */
    private void validarDevolucionCompleta(ModelVenta venta, List<ProductoDevolucion> productos)
            throws SQLException {
        if (venta == null) {
            throw new IllegalArgumentException("Venta no puede ser nula");
        }

        ValidacionElegibilidad validacion = validarElegibilidadDevolucion(venta.getIdVenta());
        if (!validacion.isElegible() && !validacion.isElegibleConAutorizacion()) {
            throw new IllegalStateException(validacion.getMensaje());
        }

        if (productos == null || productos.isEmpty()) {
            throw new IllegalArgumentException("Debe especificar productos para devolver");
        }

        for (ProductoDevolucion producto : productos) {
            producto.validar();
            verificarCantidadDisponible(producto);
        }
    }

    /**
     * Verifica que la cantidad solicitada a devolver no exceda cantidades ya
     * devueltas.
     */
    private void verificarCantidadDisponible(ProductoDevolucion producto) throws SQLException {
        String sql = "SELECT COALESCE(SUM(dd.cantidad_devuelta), 0) as total_devuelto "
                + "FROM devolucion_detalles dd "
                + "INNER JOIN devoluciones d ON dd.id_devolucion = d.id_devolucion "
                + "WHERE dd.id_detalle_venta = ? "
                + "AND d.estado IN ('aprobada', 'finalizada') "
                + "AND dd.activo = 1";

        try (Connection conn = conexion.getInstance().getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, producto.getIdDetalleVenta());

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    int totalDevuelto = rs.getInt("total_devuelto");
                    int disponible = producto.getCantidadOriginal() - totalDevuelto;

                    if (producto.getCantidadDevolver() > disponible) {
                        throw new IllegalStateException(
                                String.format("Cantidad excede lo disponible. Disponible: %d, Solicitado: %d",
                                        disponible, producto.getCantidadDevolver()));
                    }
                }
            }
        }
    }

    /**
     * CREACIÓN DE DEVOLUCIÓN PRINCIPAL
     */
    private ModelDevolucion crearDevolucionPrincipal(Connection conn, ModelVenta venta,
            String motivo, int idUsuario) throws SQLException {

        ModelDevolucion devolucion = new ModelDevolucion();
        devolucion.setNumeroDevolucion(generarNumeroDevolucion(conn));
        devolucion.setIdVenta(venta.getIdVenta());
        devolucion.setIdCliente(venta.getCliente() != null ? venta.getCliente().getIdCliente() : 1);
        devolucion.setIdUsuarioProcesa(idUsuario);
        devolucion.setFechaDevolucion(LocalDateTime.now());
        devolucion.setTipoDevolucion(ModelDevolucion.TipoDevolucion.PARCIAL);
        devolucion.setMotivo(ModelDevolucion.MotivoDevolucion.fromString(
                motivo != null ? motivo.toLowerCase().replace(" ", "_") : "otros"));
        devolucion.setObservaciones(motivo);
        devolucion.setFechaLimiteDevolucion(LocalDateTime.now().plusDays(DIAS_LIMITE_DEVOLUCION));

        String sql = "INSERT INTO devoluciones ("
                + "numero_devolucion, id_venta, id_cliente, id_usuario_procesa, "
                + "fecha_devolucion, tipo_devolucion, motivo, estado, "
                + "subtotal_devolucion, iva_devolucion, total_devolucion, "
                + "observaciones, requiere_autorizacion, fecha_limite_devolucion"
                + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, devolucion.getNumeroDevolucion());
            stmt.setInt(2, devolucion.getIdVenta());
            stmt.setInt(3, devolucion.getIdCliente());
            stmt.setInt(4, devolucion.getIdUsuarioProcesa());
            stmt.setTimestamp(5, Timestamp.valueOf(devolucion.getFechaDevolucion()));
            stmt.setString(6, devolucion.getTipoDevolucion().getValor());
            stmt.setString(7, devolucion.getMotivo().getValor());
            stmt.setString(8, devolucion.getEstado().getValor());
            stmt.setBigDecimal(9, BigDecimal.ZERO);
            stmt.setBigDecimal(10, BigDecimal.ZERO);
            stmt.setBigDecimal(11, BigDecimal.ZERO);
            stmt.setString(12, devolucion.getObservaciones());
            stmt.setBoolean(13, false);
            stmt.setTimestamp(14, Timestamp.valueOf(devolucion.getFechaLimiteDevolucion()));

            stmt.executeUpdate();

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    devolucion.setIdDevolucion(generatedKeys.getInt(1));
                } else {
                    throw new SQLException("Error obteniendo ID de devolución");
                }
            }
        }

        return devolucion;
    }

    /**
     * PROCESAMIENTO DE DETALLES
     */
    private List<ModelDetalleDevolucion> procesarDetallesProductos(Connection conn,
            ModelDevolucion devolucion, List<ProductoDevolucion> productos) throws SQLException {

        List<ModelDetalleDevolucion> detalles = new ArrayList<>();

        String sql = "INSERT INTO devolucion_detalles ("
                + "id_devolucion, id_detalle_venta, id_producto, id_variante, "
                + "cantidad_devuelta, cantidad_original, precio_unitario_original, "
                + "descuento_original, subtotal_devolucion, condicion_producto, "
                + "accion_producto, observaciones_detalle"
                + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            for (ProductoDevolucion producto : productos) {
                ModelDetalleDevolucion detalle = new ModelDetalleDevolucion();
                detalle.setIdDevolucion(devolucion.getIdDevolucion());
                detalle.setIdDetalleVenta(producto.getIdDetalleVenta());
                detalle.setIdProducto(producto.getIdProducto());
                detalle.setIdVariante(producto.getIdVariante());
                detalle.setCantidadDevuelta(producto.getCantidadDevolver());
                detalle.setCantidadOriginal(producto.getCantidadOriginal());
                detalle.setPrecioUnitarioOriginal(producto.getPrecioUnitario());
                detalle.setDescuentoOriginal(producto.getDescuento());
                detalle.setSubtotalDevolucion(producto.calcularSubtotal());
                detalle.setCondicionProducto(ModelDetalleDevolucion.CondicionProducto.NUEVO);
                detalle.setAccionProducto(ModelDetalleDevolucion.AccionProducto.REINGRESO_INVENTARIO);
                detalle.setObservacionesDetalle(producto.getObservaciones());

                stmt.setInt(1, detalle.getIdDevolucion());
                stmt.setInt(2, detalle.getIdDetalleVenta());
                stmt.setInt(3, detalle.getIdProducto());

                if (detalle.getIdVariante() > 0) {
                    stmt.setInt(4, detalle.getIdVariante());
                } else {
                    stmt.setNull(4, Types.INTEGER);
                }

                stmt.setInt(5, detalle.getCantidadDevuelta());
                stmt.setInt(6, detalle.getCantidadOriginal());
                stmt.setBigDecimal(7, detalle.getPrecioUnitarioOriginal());
                stmt.setBigDecimal(8, detalle.getDescuentoOriginal());
                stmt.setBigDecimal(9, detalle.getSubtotalDevolucion());
                stmt.setString(10, detalle.getCondicionProducto().getValor());
                stmt.setString(11, detalle.getAccionProducto().getValor());
                stmt.setString(12, detalle.getObservacionesDetalle());

                stmt.addBatch();
                detalles.add(detalle);
            }

            stmt.executeBatch();

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                int i = 0;
                while (generatedKeys.next() && i < detalles.size()) {
                    detalles.get(i).setIdDetalleDevolucion(generatedKeys.getInt(1));
                    i++;
                }
            }
        }

        return detalles;
    }

    /**
     * ACTUALIZACIÓN CRÍTICA DE INVENTARIOS
     */
    private void actualizarInventarios(Connection conn, List<ModelDetalleDevolucion> detalles)
            throws SQLException {

        String sqlVariante = "UPDATE producto_variantes "
                + "SET stock_por_pares = stock_por_pares + ? "
                + "WHERE id_variante = ?";

        String sqlProducto = "UPDATE productos "
                + "SET stock_actual = COALESCE(stock_actual, 0) + ? "
                + "WHERE id_producto = ?";

        try (PreparedStatement stmtVariante = conn.prepareStatement(sqlVariante);
                PreparedStatement stmtProducto = conn.prepareStatement(sqlProducto)) {

            for (ModelDetalleDevolucion detalle : detalles) {
                // Solo reingresar si el producto está en buen estado
                if (detalle.getCondicionProducto() == ModelDetalleDevolucion.CondicionProducto.NUEVO
                        || detalle.getCondicionProducto() == ModelDetalleDevolucion.CondicionProducto.USADO_BUENO) {

                    if (detalle.getIdVariante() > 0) {
                        // Actualizar stock de variante
                        stmtVariante.setInt(1, detalle.getCantidadDevuelta());
                        stmtVariante.setInt(2, detalle.getIdVariante());
                        stmtVariante.addBatch();

                        System.out.println("SUCCESS  Reingresando " + detalle.getCantidadDevuelta()
                                + " unidades a variante " + detalle.getIdVariante());
                    } else {
                        // Actualizar stock de producto base
                        stmtProducto.setInt(1, detalle.getCantidadDevuelta());
                        stmtProducto.setInt(2, detalle.getIdProducto());
                        stmtProducto.addBatch();

                        System.out.println("SUCCESS  Reingresando " + detalle.getCantidadDevuelta()
                                + " unidades a producto " + detalle.getIdProducto());
                    }
                } else {
                    System.out.println("WARNING  Producto en mal estado no reingresado - ID: "
                            + detalle.getIdProducto() + ", Condición: "
                            + detalle.getCondicionProducto().getDescripcion());
                }
            }

            stmtVariante.executeBatch();
            stmtProducto.executeBatch();
        }
    }

    /**
     * CÁLCULO DE TOTALES
     */
    private void calcularTotalesDevolucion(Connection conn, ModelDevolucion devolucion,
            List<ModelDetalleDevolucion> detalles) throws SQLException {

        BigDecimal subtotal = BigDecimal.ZERO;
        for (ModelDetalleDevolucion detalle : detalles) {
            subtotal = subtotal.add(detalle.getSubtotalDevolucion());
        }

        BigDecimal total = subtotal;

        devolucion.setSubtotalDevolucion(subtotal);
        devolucion.setTotalDevolucion(total);

        if (total.compareTo(MONTO_AUTORIZACION_REQUERIDA) > 0) {
            devolucion.setRequiereAutorizacion(true);
        }

        String sql = "UPDATE devoluciones SET subtotal_devolucion = ?,"
                + "total_devolucion = ?, requiere_autorizacion = ? WHERE id_devolucion = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setBigDecimal(1, devolucion.getSubtotalDevolucion());

            stmt.setBigDecimal(2, devolucion.getTotalDevolucion());
            stmt.setBoolean(3, devolucion.isRequiereAutorizacion());
            stmt.setInt(4, devolucion.getIdDevolucion());
            stmt.executeUpdate();
        }
    }

    /**
     * GENERACIÓN DE NOTA DE CRÉDITO
     */
    /**
     * GENERACIÓN DE NOTA DE CRÉDITO
     * 
     * @return El número de nota de crédito generado
     */
    private String generarNotaCredito(Connection conn, ModelDevolucion devolucion) throws SQLException {
        String numeroNotaCredito = generarNumeroNotaCredito(conn);
        String sql = "INSERT INTO notas_credito ("
                + "numero_nota_credito, id_devolucion, id_cliente, id_usuario_genera, "
                + "fecha_emision, tipo_nota, subtotal, iva, total, "
                + "estado, fecha_vencimiento, saldo_disponible, saldo_usado"
                + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        LocalDateTime fechaVencimiento = LocalDateTime.now().plusDays(30);

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, numeroNotaCredito);
            stmt.setInt(2, devolucion.getIdDevolucion());
            stmt.setInt(3, devolucion.getIdCliente());
            stmt.setInt(4, devolucion.getIdUsuarioProcesa());
            stmt.setTimestamp(5, Timestamp.valueOf(devolucion.getFechaDevolucion()));
            stmt.setString(6, "devolucion");
            stmt.setBigDecimal(7, devolucion.getSubtotalDevolucion());
            stmt.setBigDecimal(8, devolucion.getIvaDevolucion());
            stmt.setBigDecimal(9, devolucion.getTotalDevolucion());
            stmt.setString(10, "emitida");
            stmt.setTimestamp(11, Timestamp.valueOf(fechaVencimiento));
            stmt.setBigDecimal(12, devolucion.getTotalDevolucion());
            stmt.setBigDecimal(13, BigDecimal.ZERO);

            stmt.executeUpdate();

            System.out.println("SUCCESS  Nota de crédito generada: " + numeroNotaCredito
                    + " por $" + devolucion.getTotalDevolucion());

            return numeroNotaCredito;
        }
    }

    /**
     * Obtiene la Nota de Crédito completa asociada a una devolución
     * ÚTIL para mostrar el diálogo de impresión
     */
    public ModelNotaCredito obtenerNotaCreditoPorDevolucion(int idDevolucion) throws SQLException {
        String sql = "SELECT nc.*, " +
                "c.nombre as cliente_nombre, c.dni as cliente_dni, " +
                "d.numero_devolucion " +
                "FROM notas_credito nc " +
                "JOIN clientes c ON nc.id_cliente = c.id_cliente " +
                "JOIN devoluciones d ON nc.id_devolucion = d.id_devolucion " +
                "WHERE nc.id_devolucion = ? AND nc.activa = 1 LIMIT 1";

        try (Connection conn = conexion.getInstance().getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, idDevolucion);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    ModelNotaCredito nc = new ModelNotaCredito();
                    nc.setIdNotaCredito(rs.getInt("id_nota_credito"));
                    nc.setNumeroNotaCredito(rs.getString("numero_nota_credito"));
                    nc.setIdDevolucion(rs.getInt("id_devolucion"));
                    nc.setIdCliente(rs.getInt("id_cliente"));
                    nc.setIdUsuarioGenera(rs.getInt("id_usuario_genera"));
                    nc.setFechaEmision(rs.getTimestamp("fecha_emision").toLocalDateTime());
                    nc.setFechaVencimiento(rs.getTimestamp("fecha_vencimiento").toLocalDateTime());
                    nc.setSubtotal(rs.getBigDecimal("subtotal"));
                    nc.setIva(rs.getBigDecimal("iva"));
                    nc.setTotal(rs.getBigDecimal("total"));
                    nc.setSaldoDisponible(rs.getBigDecimal("saldo_disponible"));
                    nc.setSaldoUsado(rs.getBigDecimal("saldo_usado"));
                    nc.setActiva(rs.getBoolean("activa"));

                    // Enum mappings
                    String tipoStr = rs.getString("tipo_nota");
                    if (tipoStr != null) {
                        // Asumiendo que existen estos enums en ModelNotaCredito, simplificamos si no
                        try {
                            // Ajustar según tu implementación real de ModelNotaCredito
                            // nc.setTipoNota(ModelNotaCredito.TipoNota.valueOf(tipoStr.toUpperCase()));
                        } catch (Exception e) {
                        }
                    }

                    String estadoStr = rs.getString("estado");
                    if (estadoStr != null) {
                        try {
                            nc.setEstado(ModelNotaCredito.EstadoNota.valueOf(estadoStr.toUpperCase()));
                        } catch (Exception e) {
                        }
                    }

                    // Datos extra para el reporte
                    nc.setClienteNombre(rs.getString("cliente_nombre"));
                    nc.setClienteDni(rs.getString("cliente_dni"));
                    nc.setNumeroDevolucion(rs.getString("numero_devolucion"));

                    return nc;
                }
            }
        }
        return null;
    }

    /**
     * REGISTRO DE MOVIMIENTOS DE INVENTARIO
     */
    private void registrarMovimientosInventario(Connection conn,
            List<ModelDetalleDevolucion> detalles, ModelDevolucion devolucion) throws SQLException {

        String sql = "INSERT INTO inventario_movimientos ("
                + "id_producto, id_variante, tipo_movimiento, cantidad, "
                + "fecha_movimiento, id_referencia, tipo_referencia, "
                + "id_usuario, observaciones"
                + ") VALUES (?, ?, ?, ?, CURDATE(), ?, ?, ?, ?)";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {

            for (ModelDetalleDevolucion detalle : detalles) {
                // Solo registrar movimiento si se reingresa al inventario
                if (detalle.getAccionProducto() == ModelDetalleDevolucion.AccionProducto.REINGRESO_INVENTARIO) {
                    stmt.setInt(1, detalle.getIdProducto());

                    if (detalle.getIdVariante() > 0) {
                        stmt.setInt(2, detalle.getIdVariante());
                    } else {
                        stmt.setNull(2, Types.INTEGER);
                    }

                    stmt.setString(3, "entrada par"); // Siempre entrada de pares en devolución
                    stmt.setInt(4, detalle.getCantidadDevuelta());
                    stmt.setInt(5, devolucion.getIdDevolucion());
                    stmt.setString(6, "devolucion");
                    stmt.setInt(7, devolucion.getIdUsuarioProcesa());
                    stmt.setString(8, "Devolución: " + devolucion.getNumeroDevolucion());

                    stmt.addBatch();
                }
            }

            stmt.executeBatch();
        }
    }

    /**
     * MARCAR VENTA CON DEVOLUCIÓN
     */
    private void marcarVentaConDevolucion(Connection conn, int idVenta, BigDecimal montoDevuelto)
            throws SQLException {

        // Verificar si la venta fue devuelta completamente
        String sqlVerificar = "SELECT v.total, COALESCE(SUM(d.total_devolucion), 0) as total_devuelto "
                + "FROM ventas v "
                + "LEFT JOIN devoluciones d ON v.id_venta = d.id_venta "
                + "    AND d.activa = 1 AND d.estado IN ('aprobada', 'finalizada') "
                + "WHERE v.id_venta = ? "
                + "GROUP BY v.id_venta, v.total";

        try (PreparedStatement stmt = conn.prepareStatement(sqlVerificar)) {
            stmt.setInt(1, idVenta);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    BigDecimal totalVenta = rs.getBigDecimal("total");
                    BigDecimal totalDevuelto = rs.getBigDecimal("total_devuelto");

                    // Evitar división por cero
                    if (totalVenta == null || totalVenta.compareTo(BigDecimal.ZERO) == 0) {
                        return;
                    }

                    // Si la devolución total es mayor o igual al 95% del total de la venta
                    BigDecimal porcentajeDevuelto = totalDevuelto.divide(totalVenta, 4, BigDecimal.ROUND_HALF_UP);

                    if (porcentajeDevuelto.compareTo(new BigDecimal("0.95")) >= 0) {
                        // Marcar venta como devuelta completamente
                        String sqlUpdate = "UPDATE ventas SET observaciones = CONCAT(COALESCE(observaciones, ''), ' - DEVUELTA COMPLETAMENTE') WHERE id_venta = ?";
                        try (PreparedStatement updateStmt = conn.prepareStatement(sqlUpdate)) {
                            updateStmt.setInt(1, idVenta);
                            updateStmt.executeUpdate();
                        }

                        System.out.println("SUCCESS  Venta " + idVenta + " marcada como devuelta completamente");
                    }
                }
            }
        }
    }

    /**
     * UTILIDADES Y MÉTODOS AUXILIARES
     */
    private String generarNumeroDevolucion(Connection conn) throws SQLException {
        String sql = "SELECT COUNT(*) + 1 as siguiente FROM devoluciones";

        try (PreparedStatement stmt = conn.prepareStatement(sql); ResultSet rs = stmt.executeQuery()) {

            if (rs.next()) {
                int siguiente = rs.getInt("siguiente");
                return String.format("%s-%06d", PREFIJO_NUMERO_DEVOLUCION, siguiente);
            } else {
                return String.format("%s-%06d", PREFIJO_NUMERO_DEVOLUCION, 1);
            }
        }
    }

    private String generarNumeroNotaCredito(Connection conn) throws SQLException {
        String sql = "SELECT COUNT(*) + 1 as siguiente FROM notas_credito";

        try (PreparedStatement stmt = conn.prepareStatement(sql); ResultSet rs = stmt.executeQuery()) {

            if (rs.next()) {
                int siguiente = rs.getInt("siguiente");
                return String.format("NC-%06d", siguiente);
            } else {
                return String.format("NC-%06d", 1);
            }
        }
    }

    private void mostrarMensajeExito(JComponent parentComponent, ModelDevolucion devolucion) {
        String mensaje = String.format(
                "Devolución procesada exitosamente:\n\n"
                        + "Número: %s\n"
                        + "Total: $%.2f\n"
                        + "Estado: %s\n\n"
                        + "Se ha generado una nota de crédito y el inventario ha sido actualizado.",
                devolucion.getNumeroDevolucion(),
                devolucion.getTotalDevolucion(),
                devolucion.getEstado().getDescripcion());

        JOptionPane.showMessageDialog(parentComponent, mensaje,
                "Devolución Exitosa", JOptionPane.INFORMATION_MESSAGE);
    }

    // ========================================================================================
    // Sección de estadísticas, búsquedas y validaciones adicionales
    // ========================================================================================
    /**
     * Estadísticas de devoluciones
     */
    public static class EstadisticasDevolucion {

        private int pendientes;
        private int procesando;
        private int aprobadasHoy;
        private int rechazadas;
        private int autorizacionPendiente;
        private int notasPorVencer;

        public EstadisticasDevolucion() {
        }

        // Getters y setters
        public int getPendientes() {
            return pendientes;
        }

        public void setPendientes(int pendientes) {
            this.pendientes = pendientes;
        }

        public int getProcesando() {
            return procesando;
        }

        public void setProcesando(int procesando) {
            this.procesando = procesando;
        }

        public int getAprobadasHoy() {
            return aprobadasHoy;
        }

        public void setAprobadasHoy(int aprobadasHoy) {
            this.aprobadasHoy = aprobadasHoy;
        }

        public int getRechazadas() {
            return rechazadas;
        }

        public void setRechazadas(int rechazadas) {
            this.rechazadas = rechazadas;
        }

        public int getAutorizacionPendiente() {
            return autorizacionPendiente;
        }

        public void setAutorizacionPendiente(int autorizacionPendiente) {
            this.autorizacionPendiente = autorizacionPendiente;
        }

        public int getNotasPorVencer() {
            return notasPorVencer;
        }

        public void setNotasPorVencer(int notasPorVencer) {
            this.notasPorVencer = notasPorVencer;
        }
    }

    /**
     * Validación de elegibilidad para devoluciones
     */
    public static class ValidacionElegibilidad {

        private final boolean elegible;
        private final boolean elegibleConAutorizacion;
        private final int diasTranscurridos;
        private final String mensaje;

        public ValidacionElegibilidad(boolean elegible, boolean elegibleConAutorizacion,
                int diasTranscurridos, String mensaje) {
            this.elegible = elegible;
            this.elegibleConAutorizacion = elegibleConAutorizacion;
            this.diasTranscurridos = diasTranscurridos;
            this.mensaje = mensaje;
        }

        public ValidacionElegibilidad(boolean estadoValido, boolean periodoValido,
                boolean sinDevolucionesPrevias, boolean montoValido,
                int diasTranscurridos, String mensaje) {
            this.elegible = estadoValido && periodoValido && sinDevolucionesPrevias && montoValido;
            this.elegibleConAutorizacion = estadoValido && montoValido;
            this.diasTranscurridos = diasTranscurridos;
            this.mensaje = mensaje;
        }

        public boolean isElegible() {
            return elegible;
        }

        public boolean isElegibleConAutorizacion() {
            return elegibleConAutorizacion;
        }

        public int getDiasTranscurridos() {
            return diasTranscurridos;
        }

        public String getMensaje() {
            return mensaje;
        }
    }

    /**
     * Obtiene estadísticas actuales de devoluciones
     *
     * @return
     * @throws java.sql.SQLException
     * @throws java.sql.SQLException
     * @throws java.sql.SQLException
     */
    /**
     * Obtiene devoluciones recientes con mapeo completo
     */
    public List<ModelDevolucion> obtenerDevolucionesRecientes(int limite) throws SQLException {
        List<ModelDevolucion> devoluciones = new ArrayList<>();

        String sql = "SELECT d.*, "
                + "c.nombre as cliente_nombre, c.dni as cliente_dni, c.telefono as cliente_telefono, "
                + "u.nombre as usuario_nombre, "
                + "v.total as venta_total, "
                + "nc.numero_nota_credito as numero_nc, nc.saldo_disponible as saldo_nc "
                + "FROM devoluciones d "
                + "LEFT JOIN clientes c ON d.id_cliente = c.id_cliente "
                + "LEFT JOIN usuarios u ON d.id_usuario_procesa = u.id_usuario "
                + "LEFT JOIN ventas v ON d.id_venta = v.id_venta "
                + "LEFT JOIN notas_credito nc ON d.id_devolucion = nc.id_devolucion AND nc.activa = 1 "
                + "WHERE d.activa = 1 "
                + "ORDER BY d.fecha_devolucion DESC "
                + "LIMIT ?";

        try (Connection conn = conexion.getInstance().getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, limite);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    ModelDevolucion devolucion = mapearDevolucionCompleta(rs);
                    devoluciones.add(devolucion);
                }
            }
        }

        return devoluciones;
    }

    /**
     * Mapea una devolución completa desde ResultSet
     */
    private ModelDevolucion mapearDevolucionCompleta(ResultSet rs) throws SQLException {
        ModelDevolucion devolucion = new ModelDevolucion();

        devolucion.setIdDevolucion(rs.getInt("id_devolucion"));
        devolucion.setNumeroDevolucion(rs.getString("numero_devolucion"));
        devolucion.setIdVenta(rs.getInt("id_venta"));
        devolucion.setIdCliente(rs.getInt("id_cliente"));
        devolucion.setIdUsuarioProcesa(rs.getInt("id_usuario_procesa"));

        Timestamp fechaDevolucion = rs.getTimestamp("fecha_devolucion");
        if (fechaDevolucion != null) {
            devolucion.setFechaDevolucion(fechaDevolucion.toLocalDateTime());
        }

        Timestamp fechaLimite = rs.getTimestamp("fecha_limite_devolucion");
        if (fechaLimite != null) {
            devolucion.setFechaLimiteDevolucion(fechaLimite.toLocalDateTime());
        }

        devolucion.setTipoDevolucion(
                ModelDevolucion.TipoDevolucion.fromString(rs.getString("tipo_devolucion")));
        devolucion.setMotivo(
                ModelDevolucion.MotivoDevolucion.fromString(rs.getString("motivo")));
        devolucion.setEstado(
                ModelDevolucion.EstadoDevolucion.fromString(rs.getString("estado")));

        devolucion.setSubtotalDevolucion(rs.getBigDecimal("subtotal_devolucion"));
        devolucion.setIvaDevolucion(rs.getBigDecimal("iva_devolucion"));
        devolucion.setTotalDevolucion(rs.getBigDecimal("total_devolucion"));

        devolucion.setObservaciones(rs.getString("observaciones"));
        devolucion.setRequiereAutorizacion(rs.getBoolean("requiere_autorizacion"));
        devolucion.setActiva(rs.getBoolean("activa"));

        // Campos adicionales
        try {
            devolucion.setNumeroNotaCredito(rs.getString("numero_nc"));
            devolucion.setSaldoNotaCredito(rs.getBigDecimal("saldo_nc"));
        } catch (SQLException e) {
            // Ignorar si las columnas no existen (por compatibilidad)
        }

        // Mapeo de Nombres (Campos UI)
        try {
            devolucion.setNombreCliente(rs.getString("cliente_nombre"));
            devolucion.setNombreUsuarioProcesa(rs.getString("usuario_nombre"));
            devolucion.setNombreUsuarioVenta(rs.getString("vendedor_nombre"));
            devolucion.setTotalVenta(rs.getBigDecimal("venta_total"));
        } catch (SQLException e) {
            // Ignorar si no están en el select
        }

        return devolucion;
    }

    public ValidacionElegibilidad validarElegibilidadDevolucion(int idVenta) throws SQLException {
        String sql = "SELECT v.*, "
                + "DATEDIFF(CURDATE(), v.fecha_venta) as dias_transcurridos, "
                + "COUNT(DISTINCT d.id_devolucion) as devoluciones_existentes, "
                + "COALESCE(SUM(d.total_devolucion), 0) as total_devuelto, "
                + "GROUP_CONCAT(DISTINCT d.estado) as estados_devoluciones, "
                + "GROUP_CONCAT(DISTINCT d.numero_devolucion) as numeros_devoluciones "
                + "FROM ventas v "
                + "LEFT JOIN devoluciones d ON v.id_venta = d.id_venta "
                + "    AND d.activa = 1 "
                + "WHERE v.id_venta = ? "
                + "GROUP BY v.id_venta";

        try (Connection conn = conexion.getInstance().getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, idVenta);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String estado = rs.getString("estado");
                    int diasTranscurridos = rs.getInt("dias_transcurridos");
                    double totalVenta = rs.getDouble("total");
                    double totalDevuelto = rs.getDouble("total_devuelto");
                    int devolucionesExistentes = rs.getInt("devoluciones_existentes");
                    String estadosDevoluciones = rs.getString("estados_devoluciones");

                    // VALIDACIONES EN ORDEN DE PRIORIDAD
                    // 1. Verificar estado de la venta
                    boolean estadoValido = "completada".equals(estado);
                    if (!estadoValido) {
                        return new ValidacionElegibilidad(
                                false, false, diasTranscurridos,
                                "La venta debe estar en estado 'completada' para poder devolverse. Estado actual: "
                                        + estado);
                    }

                    // 2. VALIDACIÓN CRÍTICA: Verificar duplicados
                    if (devolucionesExistentes > 0) {
                        String numerosDevs = rs.getString("numeros_devoluciones");
                        return new ValidacionElegibilidad(
                                false, false, diasTranscurridos,
                                String.format("Esta venta ya tiene %d devolución(es) registrada(s).\n"
                                        + "Estados: %s\n"
                                        + "Números: %s\n"
                                        + "No se permiten devoluciones múltiples.",
                                        devolucionesExistentes, estadosDevoluciones, numerosDevs));
                    }

                    // 3. Verificar período de devolución
                    boolean periodoValido = diasTranscurridos <= DIAS_LIMITE_DEVOLUCION;

                    // 4. Verificar monto disponible
                    boolean tieneMontoDisponible = (totalVenta - totalDevuelto) > 0.01;
                    boolean montoValido = totalVenta > 0;

                    // GENERAR MENSAJE APROPIADO
                    String mensaje = generarMensajeValidacionCompleto(
                            estadoValido, periodoValido, tieneMontoDisponible,
                            montoValido, diasTranscurridos, totalVenta, totalDevuelto,
                            devolucionesExistentes);

                    boolean elegible = estadoValido && periodoValido && tieneMontoDisponible
                            && montoValido && devolucionesExistentes == 0;
                    boolean elegibleConAutorizacion = estadoValido && montoValido
                            && tieneMontoDisponible && devolucionesExistentes == 0;

                    return new ValidacionElegibilidad(elegible, elegibleConAutorizacion,
                            diasTranscurridos, mensaje);
                } else {
                    return new ValidacionElegibilidad(false, false, 0, "Venta no encontrada");
                }
            }
        }
    }

    /**
     * Genera mensaje de validación completo incluyendo información de
     * duplicados
     */
    private String generarMensajeValidacionCompleto(boolean estadoValido, boolean periodoValido,
            boolean tieneMontoDisponible, boolean montoValido,
            int diasTranscurridos, double totalVenta,
            double totalDevuelto, int devolucionesExistentes) {

        if (!estadoValido) {
            return "La venta debe estar en estado 'completada' para poder devolverse";
        }

        if (devolucionesExistentes > 0) {
            return String.format("Esta venta ya tiene %d devolución(es) registrada(s). "
                    + "No se permiten devoluciones múltiples para la misma venta.",
                    devolucionesExistentes);
        }

        if (!montoValido) {
            return "La venta no tiene un monto válido";
        }

        if (!tieneMontoDisponible) {
            return String.format("La venta ya fue devuelta completamente. "
                    + "Total: $%.2f, Devuelto: $%.2f",
                    totalVenta, totalDevuelto);
        }

        if (!periodoValido) {
            return String.format("La venta excede el período de devolución (%d días). "
                    + "Días transcurridos: %d",
                    DIAS_LIMITE_DEVOLUCION, diasTranscurridos);
        }

        double montoDisponible = totalVenta - totalDevuelto;
        return String.format("Venta elegible para devolución. Monto disponible: $%.2f",
                montoDisponible);
    }

    /**
     * Busca devoluciones según criterios
     */
    public List<ModelDevolucion> buscarDevoluciones(String numeroDevolucion, String estado,
            LocalDateTime fechaInicio, LocalDateTime fechaFin) throws SQLException {

        List<ModelDevolucion> resultados = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
                "SELECT d.*, "
                        + "c.nombre as cliente_nombre, c.dni as cliente_dni, c.telefono as cliente_telefono, "
                        + "u.nombre as usuario_nombre, "
                        + "v.total as venta_total, "
                        + "nc.numero_nota_credito as numero_nc, nc.saldo_disponible as saldo_nc "
                        + "FROM devoluciones d "
                        + "LEFT JOIN clientes c ON d.id_cliente = c.id_cliente "
                        + "LEFT JOIN usuarios u ON d.id_usuario_procesa = u.id_usuario "
                        + "LEFT JOIN ventas v ON d.id_venta = v.id_venta "
                        + "LEFT JOIN notas_credito nc ON d.id_devolucion = nc.id_devolucion AND nc.activa = 1 "
                        + "WHERE d.activa = 1");

        List<Object> parametros = new ArrayList<>();

        if (numeroDevolucion != null && !numeroDevolucion.trim().isEmpty()) {
            // MODIFICADO: Búsqueda flexible por Número, Cliente o DNI
            sql.append(" AND (d.numero_devolucion LIKE ? OR c.nombre LIKE ? OR c.dni LIKE ?)");
            String termino = "%" + numeroDevolucion.trim() + "%";
            parametros.add(termino);
            parametros.add(termino);
            parametros.add(termino);
        }

        if (estado != null && !estado.trim().isEmpty()) {
            sql.append(" AND d.estado = ?");
            parametros.add(estado);
        }

        if (fechaInicio != null) {
            sql.append(" AND d.fecha_devolucion >= ?");
            parametros.add(Timestamp.valueOf(fechaInicio));
        }

        if (fechaFin != null) {
            sql.append(" AND d.fecha_devolucion <= ?");
            parametros.add(Timestamp.valueOf(fechaFin));
        }

        sql.append(" ORDER BY d.fecha_devolucion DESC");

        try (Connection conn = conexion.getInstance().getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < parametros.size(); i++) {
                stmt.setObject(i + 1, parametros.get(i));
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    ModelDevolucion devolucion = mapearDevolucionCompleta(rs);
                    resultados.add(devolucion);
                }
            }
        }

        return resultados;
    }

    /**
     * Muestra información detallada de devolución existente al usuario
     * INTEGRACIÓN: Para usar en formularios de búsqueda y validación
     *
     * @param parentComponent Componente padre para el diálogo
     * @param idVenta         ID de la venta a consultar
     */
    public void mostrarInformacionDevolucionExistente(JComponent parentComponent, int idVenta) {
        try {
            DevolucionExistente devolucionExistente = obtenerInformacionDevolucionExistente(idVenta);

            if (devolucionExistente != null) {
                String titulo = "Devolución Existente Detectada";
                String mensaje = "Esta venta ya tiene una devolución registrada:\n\n"
                        + devolucionExistente.generarResumen() + "\n\n"
                        + "No es posible crear múltiples devoluciones para la misma venta.\n"
                        + "Consulte con el supervisor si requiere realizar ajustes.";

                JOptionPane.showMessageDialog(parentComponent, mensaje, titulo,
                        JOptionPane.WARNING_MESSAGE);
            }
        } catch (SQLException e) {
            System.err.println("Error consultando devolución existente: " + e.getMessage());
            JOptionPane.showMessageDialog(parentComponent,
                    "Error consultando historial de devoluciones: " + e.getMessage(),
                    "Error de Base de Datos", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ====================================================================
    // ESTADÍSTICAS MEJORADAS INCLUYENDO INFORMACIÓN DE DUPLICADOS
    // ====================================================================
    public EstadisticasDevolucion obtenerEstadisticasDevolucion() throws SQLException {
        EstadisticasDevolucion stats = new EstadisticasDevolucion();

        String sql = "SELECT "
                + "SUM(CASE WHEN estado = 'pendiente' THEN 1 ELSE 0 END) as pendientes, "
                + "SUM(CASE WHEN estado = 'procesando' THEN 1 ELSE 0 END) as procesando, "
                + "SUM(CASE WHEN estado = 'aprobada' AND DATE(fecha_devolucion) = CURDATE() THEN 1 ELSE 0 END) as aprobadas_hoy, "
                + "SUM(CASE WHEN estado = 'rechazada' THEN 1 ELSE 0 END) as rechazadas, "
                + "SUM(CASE WHEN requiere_autorizacion = 1 AND id_usuario_autoriza IS NULL THEN 1 ELSE 0 END) as autorizacion_pendiente, "
                + // NUEVA ESTADÍSTICA: Intentos de duplicados detectados
                "(SELECT COUNT(*) FROM devoluciones d1 "
                + " WHERE EXISTS (SELECT 1 FROM devoluciones d2 "
                + "               WHERE d1.id_venta = d2.id_venta AND d1.id_devolucion != d2.id_devolucion "
                + "               AND d1.activa = 1 AND d2.activa = 1)"
                + ") as posibles_duplicados "
                + "FROM devoluciones "
                + "WHERE activa = 1";

        // [Continúa con el resto de la implementación existente...]
        try (Connection conn = conexion.getInstance().getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery()) {

            if (rs.next()) {
                stats.setPendientes(rs.getInt("pendientes"));
                stats.setProcesando(rs.getInt("procesando"));
                stats.setAprobadasHoy(rs.getInt("aprobadas_hoy"));
                stats.setRechazadas(rs.getInt("rechazadas"));
                stats.setAutorizacionPendiente(rs.getInt("autorizacion_pendiente"));

                // NUEVA ESTADÍSTICA
                int posiblesDuplicados = rs.getInt("posibles_duplicados");
                if (posiblesDuplicados > 0) {
                    System.out.println("WARNING  Se detectaron " + posiblesDuplicados
                            + " posibles devoluciones duplicadas. Revisar integridad de datos.");
                }
            }
        }

        // Consulta adicional para notas de crédito...
        String sqlNotas = "SELECT COUNT(*) as notas_por_vencer "
                + "FROM notas_credito "
                + "WHERE fecha_vencimiento BETWEEN CURDATE() AND DATE_ADD(CURDATE(), INTERVAL 7 DAY) "
                + "AND estado = 'emitida' AND activa = 1";

        try (Connection conn = conexion.getInstance().getConnection();
                PreparedStatement stmt = conn.prepareStatement(sqlNotas);
                ResultSet rs = stmt.executeQuery()) {

            if (rs.next()) {
                stats.setNotasPorVencer(rs.getInt("notas_por_vencer"));
            }
        }

        return stats;
    }

    /**
     * Información de devolución existente para diagnóstico PRINCIPIO SRP:
     * Encapsula datos de devoluciones existentes
     */
    public static class DevolucionExistente {

        private final String numeroDevolucion;
        private final String estado;
        private final LocalDateTime fechaDevolucion;
        private final BigDecimal totalDevolucion;
        private final String tipoDevolucion;
        private final String motivo;
        private final String usuarioProcesa;
        private final int itemsDevueltos;

        public DevolucionExistente(String numeroDevolucion, String estado,
                LocalDateTime fechaDevolucion, BigDecimal totalDevolucion,
                String tipoDevolucion, String motivo,
                String usuarioProcesa, int itemsDevueltos) {
            this.numeroDevolucion = numeroDevolucion;
            this.estado = estado;
            this.fechaDevolucion = fechaDevolucion;
            this.totalDevolucion = totalDevolucion;
            this.tipoDevolucion = tipoDevolucion;
            this.motivo = motivo;
            this.usuarioProcesa = usuarioProcesa;
            this.itemsDevueltos = itemsDevueltos;
        }

        // Getters
        public String getNumeroDevolucion() {
            return numeroDevolucion;
        }

        public String getEstado() {
            return estado;
        }

        public LocalDateTime getFechaDevolucion() {
            return fechaDevolucion;
        }

        public BigDecimal getTotalDevolucion() {
            return totalDevolucion;
        }

        public String getTipoDevolucion() {
            return tipoDevolucion;
        }

        public String getMotivo() {
            return motivo;
        }

        public String getUsuarioProcesa() {
            return usuarioProcesa;
        }

        public int getItemsDevueltos() {
            return itemsDevueltos;
        }

        /**
         * Genera resumen legible para mostrar al usuario
         */
        public String generarResumen() {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

            return String.format(
                    "Devolución Existente:\n"
                            + "• Número: %s\n"
                            + "• Estado: %s\n"
                            + "• Fecha: %s\n"
                            + "• Total: $%.2f\n"
                            + "• Tipo: %s\n"
                            + "• Items: %d productos\n"
                            + "• Procesado por: %s",
                    numeroDevolucion, estado, fechaDevolucion.format(formatter),
                    totalDevolucion, tipoDevolucion, itemsDevueltos, usuarioProcesa);
        }

    }

    /**
     * ANULACIÓN DE DEVOLUCIÓN
     * Revierte el proceso: Saca productos del inventario, anula NC y cambia estado.
     */
    public boolean anularDevolucion(int idDevolucion, int idUsuario, String motivo) {
        Connection conn = null;
        try {
            conn = conexion.getInstance().getConnection();
            conn.setAutoCommit(false);

            System.out.println("Iniciando anulación de devolución " + idDevolucion);

            // 1. Obtener devolución y validar estado
            ModelDevolucion devolucion = obtenerDevolucionPorId(conn, idDevolucion);
            if (devolucion == null) {
                throw new IllegalStateException("Devolución no encontrada");
            }

            // Permitir anular APROBADAS o FINALIZADAS
            String estado = devolucion.getEstado().getValor();
            if (!"aprobada".equalsIgnoreCase(estado) && !"finalizada".equalsIgnoreCase(estado)) {
                throw new IllegalStateException(
                        "Solo se pueden anular devoluciones APROBADAS o FINALIZADAS. Estado actual: " + estado);
            }

            // 2. Obtener detalles para revertir inventario
            List<ModelDetalleDevolucion> detalles = obtenerDetallesDevolucion(conn, idDevolucion);
            Integer idBodega = raven.clases.admin.UserSession.getInstance().getIdBodegaUsuario();

            // 3. Revertir Inventario (SACAR lo que se devolvió)
            // SQL para reducir el stock que se había sumado
            String sqlInventario = "UPDATE inventario_bodega SET Stock_par = Stock_par - ? WHERE id_variante = ? AND id_bodega = ?";
            String sqlInventarioCaja = "UPDATE inventario_bodega SET Stock_caja = Stock_caja - ? WHERE id_variante = ? AND id_bodega = ?";

            try (PreparedStatement stmtInv = conn.prepareStatement(sqlInventario);
                    PreparedStatement stmtInvCaja = conn.prepareStatement(sqlInventarioCaja)) {

                for (ModelDetalleDevolucion detalle : detalles) {
                    // Solo revertir si se reingresó al inventario (NUEVO o USADO_BUENO)
                    if (detalle.getCondicionProducto() == ModelDetalleDevolucion.CondicionProducto.NUEVO ||
                            detalle.getCondicionProducto() == ModelDetalleDevolucion.CondicionProducto.USADO_BUENO) {

                        if (detalle.getIdVariante() > 0) {
                            // Revertir PARES
                            stmtInv.setInt(1, detalle.getCantidadDevuelta());
                            stmtInv.setInt(2, detalle.getIdVariante());
                            stmtInv.setInt(3, idBodega);
                            stmtInv.addBatch();

                            // Revertir CAJAS (si aplica, aunque generalmente devuelven pares)
                            // Asumimos 0 cajas por ahora para no complicar, o si la lógica original sumó
                            // cajas?
                            // La lógica original sumó "Stock_par" (línea 500). Así que solo restamos
                            // Stock_par.

                            System.out.println("Revirtiendo stock: - " + detalle.getCantidadDevuelta() + " variante "
                                    + detalle.getIdVariante());
                        }
                    }
                }
                stmtInv.executeBatch();
            }

            // 4. Actualizar Estado de Devolución a ANULADA
            String sqlEstado = "UPDATE devoluciones SET estado = 'anulada', observaciones = CONCAT(COALESCE(observaciones, ''), ' [ANULADA: ', ?, ']') WHERE id_devolucion = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sqlEstado)) {
                stmt.setString(1, motivo);
                stmt.setInt(2, idDevolucion);
                stmt.executeUpdate();
            }

            // 5. Anular Nota de Crédito (si existe)
            String sqlNC = "UPDATE notas_credito SET estado = 'anulada' WHERE id_devolucion = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sqlNC)) {
                stmt.setInt(1, idDevolucion);
                stmt.executeUpdate();
            }

            // 6. Actualizar venta para quitar marca de devolución (opcional, pero ayuda a
            // consistencia)
            // Esto es complejo porque pueden haber otras devoluciones. Lo dejamos así.

            conn.commit();
            System.out.println("Devolución anulada exitosamente.");
            return true;

        } catch (Exception e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            e.printStackTrace();
            return false;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
