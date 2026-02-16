package raven.clases.reportes;

import java.awt.Component;
import java.awt.Frame;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFrame;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.view.JasperViewer;
import raven.clases.admin.ServiceUser;
import raven.clases.admin.UserSession;
import raven.clases.comercial.ServiceCliente;
import raven.clases.principal.ServiceVenta;
import raven.clases.productos.ServiceProduct;
import raven.controlador.admin.ModelUser;
import raven.controlador.admin.SessionManager;
import raven.controlador.comercial.ModelCliente;
import raven.controlador.principal.ModelDetalleVenta;
import raven.controlador.principal.ModelVenta;

import raven.controlador.principal.conexion;
import raven.controlador.productos.ModelProduct;
import raven.modal.Toast;

/**
 * Clase que gestiona los reportes relacionados con ventas
 */
public class ReporteVentas {

    private static final Logger LOGGER = Logger.getLogger(ReporteVentas.class.getName());
    private static final String RUTA_REPORTE_FACTURA = "./src/raven/reportes/facturaSimba2Venta.jrxml";
    private static final String RUTA_REPORTE_COTIZACION = "./src/raven/reportes/cotizacionVenta.jrxml"; // New path for
                                                                                                        // quotation
                                                                                                        // report

    /**
     * Genera un reporte de ventas filtrado por rango de fechas
     *
     * @param fechaInicio fecha inicial del rango
     * @param fechaFin    fecha final del rango
     * @return Lista de ventas dentro del rango especificado
     */
    public List<Map<String, Object>> generarReporteVentasPorFecha(LocalDate fechaInicio, LocalDate fechaFin) {
        String sql = "SELECT v.id_venta, v.fecha_venta, c.nombre as cliente, "
                + "u.nombre as vendedor, v.total, v.tipo_pago, v.estado "
                + "FROM ventas v "
                + "LEFT JOIN clientes c ON v.id_cliente = c.id_cliente "
                + "JOIN usuarios u ON v.id_usuario = u.id_usuario "
                + "WHERE DATE(v.fecha_venta) BETWEEN ? AND ? "
                + "ORDER BY v.fecha_venta DESC";

        try (Connection con = conexion.getInstance().createConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setDate(1, Date.valueOf(fechaInicio));
            ps.setDate(2, Date.valueOf(fechaFin));

            try (ResultSet rs = ps.executeQuery()) {
                return mapResultSetToVentasList(rs);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al generar reporte por fecha", e);
            mostrarError("Error al generar reporte", e);
            return new ArrayList<>();
        }
    }

    /**
     * Genera un resumen estadístico de ventas dentro de un periodo
     *
     * @param fechaInicio fecha inicial del periodo
     * @param fechaFin    fecha final del periodo
     * @return Mapa con datos estadísticos de ventas
     */
    public Map<String, Object> generarResumenVentas(LocalDate fechaInicio, LocalDate fechaFin) {
        // Por defecto, intentar obtener el usuario actual de la sesión
        ModelUser usuario = null;
        try {
            if (SessionManager.getInstance().isSessionActive()) {
                usuario = SessionManager.getInstance().getCurrentUser();
            }
        } catch (Exception e) {
            // Ignorar error al obtener usuario
        }
        return generarResumenVentas(fechaInicio, fechaFin, usuario);
    }

    /**
     * Genera un resumen estadístico de ventas dentro de un periodo con filtro de
     * bodega
     *
     * @param fechaInicio fecha inicial del periodo
     * @param fechaFin    fecha final del periodo
     * @param usuario     Usuario actual para filtrar por bodega
     * @return Mapa con datos estadísticos de ventas
     */
    public Map<String, Object> generarResumenVentas(LocalDate fechaInicio, LocalDate fechaFin, ModelUser usuario) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ")
                .append("COUNT(*) as total_ventas, ")
                .append("SUM(v.total) as monto_total, ")
                .append("AVG(v.total) as promedio_venta, ")
                .append("SUM(CASE WHEN v.tipo_pago = 'efectivo' THEN v.total ELSE 0 END) as efectivo, ")
                .append("SUM(CASE WHEN v.tipo_pago = 'tarjeta' THEN v.total ELSE 0 END) as tarjeta, ")
                .append("SUM(CASE WHEN v.tipo_pago = 'transferencia' THEN v.total ELSE 0 END) as transferencia ")
                .append("FROM ventas v ")
                .append("JOIN usuarios u ON v.id_usuario = u.id_usuario ")
                .append("WHERE DATE(v.fecha_venta) BETWEEN ? AND ? AND v.estado = 'completada'");

        Integer idBodega = null;
        if (usuario != null && !"admin".equalsIgnoreCase(usuario.getRol())) {
            idBodega = usuario.getIdBodega();
            if (idBodega != null) {
                sql.append(" AND u.id_bodega = ?");
            }
        }

        Map<String, Object> resumen = new HashMap<>();

        try (Connection con = conexion.getInstance().createConnection();
                PreparedStatement ps = con.prepareStatement(sql.toString())) {

            ps.setDate(1, Date.valueOf(fechaInicio));
            ps.setDate(2, Date.valueOf(fechaFin));

            if (idBodega != null) {
                ps.setInt(3, idBodega);
            }

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    resumen.put("total_ventas", rs.getInt("total_ventas"));
                    resumen.put("monto_total", rs.getDouble("monto_total"));
                    resumen.put("promedio_venta", rs.getDouble("promedio_venta"));
                    resumen.put("efectivo", rs.getDouble("efectivo"));
                    resumen.put("tarjeta", rs.getDouble("tarjeta"));
                    resumen.put("transferencia", rs.getDouble("transferencia"));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al generar resumen de ventas", e);
            mostrarError("Error al generar resumen", e);
        }

        return resumen;
    }

    /**
     * Obtiene el detalle de una venta específica
     *
     * @param idVenta identificador de la venta
     * @return Lista con los detalles de la venta
     */
    public List<Map<String, Object>> generarDetalleVenta(int idVenta) {
        String sql = "SELECT vd.id_detalle, vd.id_producto, p.nombre as producto, "
                + "vd.cantidad, vd.precio_unitario, vd.descuento as descuento_detalle, "
                + "v.descuento as descuento_venta, "
                + "vd.subtotal, vd.activo, "
                + "c.nombre as color, CONCAT(t.numero, ' ', COALESCE(t.sistema,'')) as talla, "
                + "COALESCE(pv.ean, '') as barcode "
                + "FROM venta_detalles vd "
                + "JOIN productos p ON vd.id_producto = p.id_producto "
                + "JOIN ventas v ON vd.id_venta = v.id_venta "
                + "LEFT JOIN producto_variantes pv ON vd.id_variante = pv.id_variante "
                + "LEFT JOIN colores c ON pv.id_color = c.id_color "
                + "LEFT JOIN tallas t ON pv.id_talla = t.id_talla "
                + "WHERE vd.id_venta = ?";
        try (Connection con = conexion.getInstance().createConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idVenta);

            try (ResultSet rs = ps.executeQuery()) {
                return mapResultSetToDetalleVentaList(rs);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al generar detalle de venta", e);
            mostrarError("Error al obtener detalles de venta", e);
            return new ArrayList<>();
        }
    }

    /**
     * Genera reporte de ventas agrupado por producto
     *
     * @param fechaInicio fecha inicial del periodo
     * @param fechaFin    fecha final del periodo
     * @return Lista de productos vendidos con estadísticas
     */
    public List<Map<String, Object>> generarReporteVentasPorProducto(LocalDate fechaInicio, LocalDate fechaFin) {
        String sql = "SELECT p.id_producto, p.nombre as producto, "
                + "SUM(CASE "
                + "  WHEN LOWER(vd.tipo_venta) = 'caja' THEN vd.cantidad * COALESCE(NULLIF(p.pares_por_caja, 0), 12) "
                + "  ELSE vd.cantidad "
                + "END) as cantidad_vendida, "
                + "SUM(vd.subtotal) as total_vendido, "
                + "m.nombre as marca, cat.nombre as categoria "
                + "FROM venta_detalles vd "
                + "JOIN productos p ON vd.id_producto = p.id_producto "
                + "JOIN ventas v ON vd.id_venta = v.id_venta "
                + "JOIN marcas m ON p.id_marca = m.id_marca "
                + "JOIN categorias cat ON p.id_categoria = cat.id_categoria "
                + "WHERE DATE(v.fecha_venta) BETWEEN ? AND ? AND v.estado = 'completada' "
                + "GROUP BY p.id_producto "
                + "ORDER BY total_vendido DESC";

        try (Connection con = conexion.getInstance().createConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setDate(1, Date.valueOf(fechaInicio));
            ps.setDate(2, Date.valueOf(fechaFin));

            try (ResultSet rs = ps.executeQuery()) {
                return mapResultSetToProductoVentaList(rs);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al generar reporte por producto", e);
            mostrarError("Error al generar reporte por producto", e);
            return new ArrayList<>();
        }
    }

    /**
     * Genera reporte de ventas agrupado por vendedor
     *
     * @param fechaInicio fecha inicial del periodo
     * @param fechaFin    fecha final del periodo
     * @return Lista de vendedores con sus estadísticas de ventas
     */
    public List<Map<String, Object>> generarReporteVentasPorVendedor(LocalDate fechaInicio, LocalDate fechaFin) {
        String sql = "SELECT u.id_usuario, u.nombre as vendedor, "
                + "COUNT(v.id_venta) as total_ventas, "
                + "SUM(v.total) as monto_total, "
                + "AVG(v.total) as promedio_venta "
                + "FROM ventas v "
                + "JOIN usuarios u ON v.id_usuario = u.id_usuario "
                + "WHERE DATE(v.fecha_venta) BETWEEN ? AND ? AND v.estado = 'completada' "
                + "GROUP BY u.id_usuario "
                + "ORDER BY monto_total DESC";

        try (Connection con = conexion.getInstance().createConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setDate(1, Date.valueOf(fechaInicio));
            ps.setDate(2, Date.valueOf(fechaFin));

            try (ResultSet rs = ps.executeQuery()) {
                return mapResultSetToVendedorVentaList(rs);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al generar reporte por vendedor", e);
            mostrarError("Error al generar reporte por vendedor", e);
            return new ArrayList<>();
        }
    }

    /**
     * Obtiene estadísticas de ventas para un mes específico
     *
     * @param year  año
     * @param month mes (1-12)
     * @return Mapa con estadísticas de ventas del mes
     */
    public Map<String, Object> obtenerEstadisticasVentasMes(int year, int month) {
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate fechaInicio = yearMonth.atDay(1);
        LocalDate fechaFin = yearMonth.atEndOfMonth();

        // Obtener el id_bodega del usuario actual
        int idUsuarioActual = obtenerIdUsuarioActual();
        int idBodega;

        try (Connection conTemp = conexion.getInstance().createConnection()) {
            ModelUser usuarioActual = obtenerUsuario(idUsuarioActual, conTemp);
            idBodega = usuarioActual.getIdBodega();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al obtener bodega del usuario", e);
            mostrarError("Error al obtener bodega del usuario", e);
            return new HashMap<>();
        }

        Map<String, Object> estadisticas = new HashMap<>();
        estadisticas.put("mes", month);
        estadisticas.put("año", year);

        // CONSULTA 1: Estadísticas generales de VENTAS (evita duplicidad por joins)
        // Se usa DISTINCT id_venta en el count por seguridad, aunque con el join a
        // usuarios (1:1) no debería duplicar.
        String sqlVentas = "SELECT "
                + "COUNT(DISTINCT v.id_venta) as cantidad_ventas, "
                + "SUM(v.total) as total_ventas, "
                + "AVG(v.total) as promedio_venta, "
                + "MAX(v.total) as venta_maxima, "
                + "MIN(v.total) as venta_minima "
                + "FROM ventas v "
                + "JOIN usuarios u ON v.id_usuario = u.id_usuario "
                + "WHERE v.fecha_venta BETWEEN ? AND ? "
                + "AND v.estado = 'completada' "
                + "AND u.id_bodega = ?";

        // CONSULTA 2: Cantidad total de PRODUCTOS vendidos (usando tipo_venta)
        String sqlProductos = "SELECT "
                + "SUM(CASE "
                + "  WHEN LOWER(vd.tipo_venta) = 'caja' THEN vd.cantidad * COALESCE(NULLIF(p.pares_por_caja, 0), 12) " // Fallback
                                                                                                                       // a
                                                                                                                       // 12
                                                                                                                       // si
                                                                                                                       // nulo
                                                                                                                       // o
                                                                                                                       // 0
                + "  ELSE vd.cantidad "
                + "END) as total_productos_vendidos "
                + "FROM venta_detalles vd "
                + "JOIN ventas v ON vd.id_venta = v.id_venta "
                + "JOIN productos p ON vd.id_producto = p.id_producto "
                + "JOIN usuarios u ON v.id_usuario = u.id_usuario "
                + "WHERE v.fecha_venta BETWEEN ? AND ? "
                + "AND v.estado = 'completada' "
                + "AND u.id_bodega = ?";

        try (Connection con = conexion.getInstance().createConnection()) {

            // Ejecutar consulta de Ventas
            try (PreparedStatement psVentas = con.prepareStatement(sqlVentas)) {
                psVentas.setDate(1, Date.valueOf(fechaInicio));
                psVentas.setDate(2, Date.valueOf(fechaFin));
                psVentas.setInt(3, idBodega);

                try (ResultSet rs = psVentas.executeQuery()) {
                    if (rs.next()) {
                        estadisticas.put("cantidad_ventas", rs.getInt("cantidad_ventas"));
                        estadisticas.put("total_ventas", rs.getDouble("total_ventas"));
                        estadisticas.put("promedio_venta", rs.getDouble("promedio_venta"));
                        estadisticas.put("venta_maxima", rs.getDouble("venta_maxima"));
                        estadisticas.put("venta_minima", rs.getDouble("venta_minima"));
                    }
                }
            }

            // Ejecutar consulta de Productos
            try (PreparedStatement psProd = con.prepareStatement(sqlProductos)) {
                psProd.setDate(1, Date.valueOf(fechaInicio));
                psProd.setDate(2, Date.valueOf(fechaFin));
                psProd.setInt(3, idBodega);

                try (ResultSet rs = psProd.executeQuery()) {
                    if (rs.next()) {
                        estadisticas.put("total_productos_vendidos", rs.getInt("total_productos_vendidos"));
                    } else {
                        estadisticas.put("total_productos_vendidos", 0);
                    }
                }
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al obtener estadísticas de ventas del mes", e);
            mostrarError("Error al obtener estadísticas", e);
        }

        return estadisticas;
    }

    /**
     * Filtra ventas según múltiples criterios
     *
     * @param fechaInicio fecha inicial (opcional)
     * @param fechaFin    fecha final (opcional)
     * @param idCliente   ID del cliente (opcional, -1 para ignorar)
     * @param estado      estado de la venta (opcional)
     * @return Lista de ventas que cumplen los criterios
     */
    public List<Map<String, Object>> filtrarVentas(LocalDate fechaInicio, LocalDate fechaFin,
            int idCliente, String estado) {

        // Obtener el id_bodega del usuario actual
        int idUsuarioActual = obtenerIdUsuarioActual();

        StringBuilder sql = new StringBuilder(
                "SELECT v.id_venta, v.fecha_venta, c.nombre as cliente, "
                        + "u.nombre as vendedor, v.total, v.tipo_pago, v.estado, v.descuento "
                        + "FROM ventas v "
                        + "LEFT JOIN clientes c ON v.id_cliente = c.id_cliente "
                        + "JOIN usuarios u ON v.id_usuario = u.id_usuario "
                        + "WHERE u.id_bodega = ? "); // Filtrar por bodega del usuario actual

        List<Object> parametros = new ArrayList<>();

        // Obtener id_bodega del usuario actual
        try (Connection conTemp = conexion.getInstance().createConnection()) {
            ModelUser usuarioActual = obtenerUsuario(idUsuarioActual, conTemp);
            parametros.add(usuarioActual.getIdBodega());
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al obtener bodega del usuario", e);
            mostrarError("Error al obtener bodega del usuario", e);
            return new ArrayList<>();
        }

        // Añadir condiciones según parámetros proporcionados
        if (fechaInicio != null && fechaFin != null) {
            sql.append(" AND DATE(v.fecha_venta) BETWEEN ? AND ? ");
            parametros.add(Date.valueOf(fechaInicio));
            parametros.add(Date.valueOf(fechaFin));
        }

        if (idCliente != -1) {
            sql.append(" AND v.id_cliente = ? ");
            parametros.add(idCliente);
        }

        if (estado != null && !estado.isEmpty()) {
            sql.append(" AND v.estado = ? ");
            parametros.add(estado);
        }

        sql.append(" ORDER BY v.fecha_venta DESC");

        try (Connection con = conexion.getInstance().createConnection();
                PreparedStatement ps = con.prepareStatement(sql.toString())) {

            // Establecer los parámetros en la consulta preparada
            for (int i = 0; i < parametros.size(); i++) {
                ps.setObject(i + 1, parametros.get(i));
            }

            try (ResultSet rs = ps.executeQuery()) {
                return mapResultSetToVentasList(rs);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al filtrar ventas", e);
            mostrarError("Error al filtrar ventas", e);
            return new ArrayList<>();
        }
    }

    /**
     * Genera y muestra un informe de factura
     *
     * @param idVenta ID de la venta para generar factura
     */
    public void llamarInforme(int idVenta) {
        Connection con = null;
        try {
            con = conexion.getInstance().createConnection();
            HashMap<String, Object> parametros = new HashMap<>();
            parametros.put("idventaV", idVenta);

            JasperReport jasperReport = null;

            // Intentar primero cargar el archivo compilado (.jasper)
            try {
                java.io.InputStream jasperStream = getClass()
                        .getResourceAsStream("/raven/reportes/facturaSimba2Venta.jasper");
                if (jasperStream != null) {
                    jasperReport = (JasperReport) net.sf.jasperreports.engine.util.JRLoader.loadObject(jasperStream);
                    LOGGER.log(Level.INFO, "Reporte cargado desde .jasper");
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "No se pudo cargar .jasper, intentando compilar .jrxml", e);
            }

            // Si no se pudo cargar el .jasper, compilar el .jrxml
            if (jasperReport == null) {
                java.io.InputStream jrxmlStream = getClass()
                        .getResourceAsStream("/raven/reportes/facturaSimba2Venta.jrxml");

                if (jrxmlStream == null) {
                    LOGGER.log(Level.SEVERE,
                            "No se encontró el archivo de reporte: /raven/reportes/facturaSimba2Venta.jrxml");
                    mostrarError("Error al generar factura", new Exception(
                            "Plantilla de factura no encontrada. Verifique que el archivo facturaSimba2Venta.jrxml existe en src/raven/reportes/"));
                    return;
                }

                LOGGER.log(Level.INFO, "Compilando reporte desde .jrxml");
                jasperReport = JasperCompileManager.compileReport(jrxmlStream);
            }

            JasperPrint jp = JasperFillManager.fillReport(jasperReport, parametros, con);
            JasperViewer view = new JasperViewer(jp, false);
            view.setTitle("Factura " + idVenta);
            view.setExtendedState(Frame.MAXIMIZED_BOTH);
            view.setVisible(true);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al generar informe de factura", e);
            e.printStackTrace(); // Imprimir stack trace completo para debugging
            mostrarError("Error al generar factura", e);
        } finally {
            if (con != null) {
                try {
                    con.close();
                } catch (SQLException e) {
                    LOGGER.log(Level.WARNING, "Error al cerrar conexión", e);
                }
            }
        }
    }
    // New method for quotation

    // Método para generar una cotización
    public void generarCotizacion(int idVenta, Component parentComponent) {
        Connection con = null;
        try {
            con = conexion.getInstance().createConnection();
            // Verificar si el documento es una cotización
            if (!esDocumentoCotizacion(idVenta, con)) {
                // Usar el componente principal de la aplicación como padre

                JOptionPane.showMessageDialog(parentComponent,
                        "El documento no es una cotización",
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            HashMap<String, Object> parametros = new HashMap<>();
            parametros.put("idventaV", idVenta);

            JasperReport jasperReport = null;

            // Intentar primero cargar el archivo compilado (.jasper)
            try {
                java.io.InputStream jasperStream = getClass()
                        .getResourceAsStream("/raven/reportes/cotizacionVenta.jasper");
                if (jasperStream != null) {
                    jasperReport = (JasperReport) net.sf.jasperreports.engine.util.JRLoader.loadObject(jasperStream);
                    LOGGER.log(Level.INFO, "Reporte de cotización cargado desde .jasper");
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "No se pudo cargar cotización .jasper, intentando compilar .jrxml", e);
            }

            // Si no se pudo cargar el .jasper, compilar el .jrxml
            if (jasperReport == null) {
                java.io.InputStream jrxmlStream = getClass()
                        .getResourceAsStream("/raven/reportes/cotizacionVenta.jrxml");

                if (jrxmlStream == null) {
                    LOGGER.log(Level.SEVERE,
                            "No se encontró el archivo de reporte: /raven/reportes/cotizacionVenta.jrxml");
                    JOptionPane.showMessageDialog(parentComponent,
                            "Plantilla de cotización no encontrada. Verifique que el archivo cotizacionVenta.jrxml existe en src/raven/reportes/",
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }

                LOGGER.log(Level.INFO, "Compilando reporte de cotización desde .jrxml");
                jasperReport = JasperCompileManager.compileReport(jrxmlStream);
            }

            JasperPrint jp = JasperFillManager.fillReport(jasperReport, parametros, con);
            JasperViewer view = new JasperViewer(jp, false);
            view.setTitle("Cotización #" + idVenta);
            view.setExtendedState(Frame.MAXIMIZED_BOTH);
            view.setVisible(true);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al generar cotización", e);
            e.printStackTrace(); // Imprimir stack trace completo para debugging

            // Mostrar mensaje de error de forma segura
            SwingUtilities.invokeLater(() -> {

                JOptionPane.showMessageDialog(parentComponent,
                        "Error al generar cotización: " + e.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
            });
        } finally {
            if (con != null) {
                try {
                    con.close();
                } catch (SQLException e) {
                    LOGGER.log(Level.WARNING, "Error al cerrar conexión", e);
                }
            }
        }
    }

    private boolean esDocumentoCotizacion(int idVenta, Connection con) throws SQLException {
        String sql = "SELECT es_cotizacion FROM ventas WHERE id_venta = ?";
        try (PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, idVenta);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getBoolean("es_cotizacion");
                }
            }
        }
        return false;
    }

    /**
     * Mapea un ResultSet a una lista de ventas
     */
    private List<Map<String, Object>> mapResultSetToVentasList(ResultSet rs) throws SQLException {
        List<Map<String, Object>> lista = new ArrayList<>();
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm");

        while (rs.next()) {
            Map<String, Object> fila = new HashMap<>();
            fila.put("id_venta", rs.getInt("id_venta"));

            // Usar Timestamp para incluir hora
            java.sql.Timestamp fechaTimestamp = rs.getTimestamp("fecha_venta");
            String fechaFormateada = fechaTimestamp != null ? sdf.format(fechaTimestamp) : "";
            fila.put("fecha_venta", fechaFormateada);

            fila.put("cliente", rs.getString("cliente"));
            fila.put("vendedor", rs.getString("vendedor"));
            fila.put("descuento", rs.getDouble("descuento"));
            fila.put("total", rs.getDouble("total"));
            fila.put("tipo_pago", rs.getString("tipo_pago"));
            fila.put("estado", rs.getString("estado"));
            lista.add(fila);
        }
        return lista;
    }

    /**
     * Mapea un ResultSet a una lista de detalles de venta
     */
    private List<Map<String, Object>> mapResultSetToDetalleVentaList(ResultSet rs) throws SQLException {
        List<Map<String, Object>> lista = new ArrayList<>();
        double descuentoVenta = 0; // Variable para almacenar el descuento de venta

        while (rs.next()) {
            Map<String, Object> fila = new HashMap<>();
            fila.put("id_detalle", rs.getInt("id_detalle"));
            fila.put("id_producto", rs.getInt("id_producto"));
            fila.put("producto", rs.getString("producto"));
            fila.put("cantidad", rs.getInt("cantidad"));
            fila.put("precio_unitario", rs.getBigDecimal("precio_unitario"));
            fila.put("descuento_detalle", rs.getBigDecimal("descuento_detalle")); // Descuento por producto
            fila.put("descuento_venta", rs.getBigDecimal("descuento_venta")); // Descuento de la venta completa
            fila.put("subtotal", rs.getBigDecimal("subtotal"));

            fila.put("color", rs.getString("color"));
            fila.put("talla", rs.getString("talla"));
            fila.put("barcode", rs.getString("barcode"));
            fila.put("activo", rs.getInt("activo"));

            // Guardar el descuento de venta (sólo necesitas capturarlo una vez)
            descuentoVenta = rs.getDouble("descuento_venta");

            lista.add(fila);
        }

        // Si hay elementos en la lista, añade el descuento de venta al resultado
        if (!lista.isEmpty()) {
            lista.get(0).put("descuento_venta_total", descuentoVenta);
        }

        return lista;
    }

    /**
     * Mapea un ResultSet a una lista de productos vendidos
     */
    private List<Map<String, Object>> mapResultSetToProductoVentaList(ResultSet rs) throws SQLException {
        List<Map<String, Object>> lista = new ArrayList<>();
        while (rs.next()) {
            Map<String, Object> fila = new HashMap<>();
            fila.put("id_producto", rs.getInt("id_producto"));
            fila.put("producto", rs.getString("producto"));
            fila.put("cantidad_vendida", rs.getInt("cantidad_vendida"));
            fila.put("total_vendido", rs.getDouble("total_vendido"));
            fila.put("marca", rs.getString("marca"));
            fila.put("categoria", rs.getString("categoria"));
            lista.add(fila);
        }
        return lista;
    }

    /**
     * Mapea un ResultSet a una lista de vendedores con estadísticas
     */
    private List<Map<String, Object>> mapResultSetToVendedorVentaList(ResultSet rs) throws SQLException {
        List<Map<String, Object>> lista = new ArrayList<>();
        while (rs.next()) {
            Map<String, Object> fila = new HashMap<>();
            fila.put("id_usuario", rs.getInt("id_usuario"));
            fila.put("vendedor", rs.getString("vendedor"));
            fila.put("total_ventas", rs.getInt("total_ventas"));
            fila.put("monto_total", rs.getDouble("monto_total"));
            fila.put("promedio_venta", rs.getDouble("promedio_venta"));
            lista.add(fila);
        }
        return lista;
    }

    /**
     * Muestra un mensaje de error al usuario
     */
    private void mostrarError(String mensaje, Exception e) {
        JOptionPane.showMessageDialog(null, mensaje + ": " + e.getMessage());

    }
    // Método para crear una cotización

    public int crearCotizacion(ModelVenta cotizacion) throws SQLException {
        Connection con = null;
        try {
            con = conexion.getInstance().createConnection();
            con.setAutoCommit(false);

            String tipoPago = cotizacion.getTipoPago();
            if (tipoPago == null || tipoPago.isEmpty()) {

                tipoPago = "efectivo";

            }

            // Insertar la cotización como un tipo especial de venta
            String sqlCotizacion = "INSERT INTO ventas "
                    + "(id_cliente, id_usuario, id_caja, id_movimiento, fecha_venta, "
                    + "subtotal, descuento, iva, total, estado, tipo_pago, observaciones, es_cotizacion) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

            try (PreparedStatement pCotizacion = con.prepareStatement(sqlCotizacion, Statement.RETURN_GENERATED_KEYS)) {
                // Establecer los parámetros
                pCotizacion.setObject(1,
                        cotizacion.getCliente() != null ? cotizacion.getCliente().getIdCliente() : null);
                pCotizacion.setInt(2, cotizacion.getUsuario().getIdUsuario());

                // Usar caja y movimiento por defecto para cotizaciones
                // Logica para
                // cajas------------------------------------------------------------------------------------
                pCotizacion.setInt(3, 1);
                pCotizacion.setInt(4, 1);

                pCotizacion.setDate(5, Date.valueOf(cotizacion.getFechaVenta().toLocalDate()));
                pCotizacion.setDouble(6, cotizacion.getSubtotal());
                pCotizacion.setDouble(7, cotizacion.getDescuento());
                pCotizacion.setDouble(8, 0.0); // IVA para cotización
                pCotizacion.setDouble(9, cotizacion.getTotal());
                pCotizacion.setString(10, "cotizacion"); // Estado inicial
                pCotizacion.setString(11, tipoPago); // Tipo de pago para cotización
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

                // Insertar detalles de la cotización
                for (ModelDetalleVenta detalle : cotizacion.getDetalles()) {
                    String sqlDetalle = "INSERT INTO venta_detalles "
                            + "(id_venta, id_producto, cantidad, precio_unitario, descuento, subtotal,cantidad_cotizacion) "
                            + "VALUES (?, ?, ?, ?, ?, ?,?)";
                    try (PreparedStatement pDetalle = con.prepareStatement(sqlDetalle)) {
                        pDetalle.setInt(1, idCotizacion);
                        pDetalle.setInt(2, detalle.getProducto().getProductId());
                        pDetalle.setInt(3, detalle.getCantidad());
                        pDetalle.setDouble(4, detalle.getPrecioUnitario());
                        pDetalle.setDouble(5, detalle.getDescuento());
                        pDetalle.setDouble(6, detalle.getSubtotal());
                        if (detalle.getTipoVenta().toLowerCase().equals("caja")) {
                            int cantidadC = detalle.getCantidad() * 24;
                            pDetalle.setInt(7, cantidadC);
                        } else {
                            pDetalle.setInt(7, detalle.getCantidad());

                        }

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

    // Método para convertir una cotización a venta
    public void convertirCotizacionAVenta(int idCotizacion) throws SQLException {
        Connection con = null;
        try {
            con = conexion.getInstance().createConnection();
            con.setAutoCommit(false);

            // Obtener datos de la cotización antes de modificarla
            ModelVenta venta = obtenerCotizacion(idCotizacion, con);

            if (venta.getDetalles() == null || venta.getDetalles().isEmpty()) {
                throw new SQLException(
                        "No se pudieron recuperar los detalles de la cotización o la cotización no existe.");
            }

            // Eliminar la cotización original para evitar duplicidad
            // Primero eliminar detalles
            String sqlDeleteDetalles = "DELETE FROM venta_detalles WHERE id_venta = ?";
            try (PreparedStatement pDeleteDetalles = con.prepareStatement(sqlDeleteDetalles)) {
                pDeleteDetalles.setInt(1, idCotizacion);
                pDeleteDetalles.executeUpdate();
            }

            // Eliminar encabezado
            String sqlDeleteVenta = "DELETE FROM ventas WHERE id_venta = ?";
            try (PreparedStatement pDeleteVenta = con.prepareStatement(sqlDeleteVenta)) {
                pDeleteVenta.setInt(1, idCotizacion);
                pDeleteVenta.executeUpdate();
            }

            // Cambiar estado y tipo de documento para la nueva venta
            venta.setEsCotizacion(false);
            venta.setEstado("completada");

            // Establecer ID a 0 para que se genere uno nuevo (o null si es Integer)
            venta.setIdVenta(0);

            // Generar venta nueva (esto registrará movimientos de inventario)
            ServiceVenta serviceVenta = new ServiceVenta();
            serviceVenta.crearVenta(venta);

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

    // Método auxiliar para obtener caja por defecto
    private int obtenerCajaPorDefecto(Connection con) throws SQLException {
        String sql = "SELECT id_caja FROM cajas WHERE activa = 1 LIMIT 1";
        try (PreparedStatement stmt = con.prepareStatement(sql); ResultSet rs = stmt.executeQuery()) {

            if (rs.next()) {
                return rs.getInt("id_caja");
            } else {
                throw new SQLException("No se encontró una caja activa");
            }
        }
    }

    // Método auxiliar para obtener movimiento por defecto
    private int obtenerMovimientoPorDefecto(Connection con) throws SQLException {
        String sql = "SELECT id_movimiento FROM caja_movimientos WHERE fecha_cierre IS NULL LIMIT 1";
        try (PreparedStatement stmt = con.prepareStatement(sql); ResultSet rs = stmt.executeQuery()) {

            if (rs.next()) {
                return rs.getInt("id_movimiento");
            } else {
                throw new SQLException("No se encontró un movimiento de caja abierto");
            }
        }
    }

    // Método para obtener una cotización existente
    private ModelVenta obtenerCotizacion(int idCotizacion, Connection con) throws SQLException {
        ModelVenta venta = new ModelVenta();

        // Consultar datos de la cotización
        String sqlCotizacion = "SELECT * FROM ventas WHERE id_venta = ? AND es_cotizacion = true";
        try (PreparedStatement pCotizacion = con.prepareStatement(sqlCotizacion)) {
            pCotizacion.setInt(1, idCotizacion);
            try (ResultSet rsCotizacion = pCotizacion.executeQuery()) {
                if (rsCotizacion.next()) {
                    // Mapear campos de cotización a venta
                    // Asegúrate de tener métodos para obtener cliente, usuario, etc.
                    venta.setCliente(obtenerCliente(rsCotizacion.getInt("id_cliente"), con));
                    venta.setUsuario(obtenerUsuario(rsCotizacion.getInt("id_usuario"), con));
                    venta.setFechaVenta(rsCotizacion.getTimestamp("fecha_venta").toLocalDateTime());
                    venta.setSubtotal(rsCotizacion.getDouble("subtotal"));
                    venta.setDescuento(rsCotizacion.getDouble("descuento"));
                    venta.setTotal(rsCotizacion.getDouble("total"));
                    venta.setObservaciones(rsCotizacion.getString("observaciones"));
                    venta.setEsCotizacion(false);
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

                    detalles.add(detalle);
                }
            }
        }

        venta.setDetalles(detalles);
        return venta;
    }

    private ModelProduct obtenerProducto(int idProducto, Connection con) throws SQLException {
        // Usar ServiceProduct para obtener el producto
        ServiceProduct serviceProduct = new ServiceProduct();
        return serviceProduct.getProductById(idProducto);
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

    private int obtenerIdUsuarioActual() {
        try {
            if (UserSession.getInstance().getCurrentUser() != null) {
                return UserSession.getInstance().getCurrentUser().getIdUsuario();
            }
        } catch (Throwable ignore) {
        }
        try {
            if (SessionManager.getInstance().isSessionActive()
                    && SessionManager.getInstance().getCurrentUser() != null) {
                return SessionManager.getInstance().getCurrentUser().getIdUsuario();
            }
        } catch (Throwable ignore) {
        }
        return 1; // fallback para entorno de desarrollo
    }
}