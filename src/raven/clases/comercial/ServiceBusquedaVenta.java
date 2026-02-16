package raven.clases.comercial;

import raven.controlador.principal.conexion;
import raven.controlador.principal.ModelVenta;
import raven.controlador.comercial.ModelCliente;
import raven.controlador.admin.ModelUser;
import raven.clases.principal.ServiceVenta;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Servicio especializado para búsqueda de ventas en el sistema de devoluciones
 * Implementa búsqueda avanzada con criterios múltiples y validación de
 * elegibilidad
 *
 * APLICANDO PRINCIPIOS SOLID: - SRP: Responsabilidad única de búsqueda de
 * ventas - OCP: Extensible para nuevos criterios de búsqueda - DIP: Depende de
 * abstracciones (ServiceVenta)
 */
public class ServiceBusquedaVenta {

    // ====================================================================
    // CONSTANTES Y CONFIGURACIÓN
    // ====================================================================
    private static final Logger LOGGER = Logger.getLogger(ServiceBusquedaVenta.class.getName());
    private static final int DIAS_LIMITE_DEVOLUCION = 30;
    private static final int LIMITE_RESULTADOS = 50;

    // ====================================================================
    // DEPENDENCIAS - APLICANDO DEPENDENCY INJECTION
    // ====================================================================
    private final ServiceVenta serviceVenta;

    /**
     * Constructor con inyección de dependencias PRINCIPIO DIP: Depende de
     * abstracción, no de implementación concreta
     */
    public ServiceBusquedaVenta() {
        this.serviceVenta = new ServiceVenta();
    }

    // ====================================================================
    // MÉTODOS DE BÚSQUEDA PRINCIPALES
    // ====================================================================
    /**
     * Busca ventas por criterio múltiple con validación de elegibilidad
     *
     * @param criterio Texto de búsqueda (ID venta, DNI cliente, nombre, etc.)
     * @return Lista de resultados de búsqueda con información de elegibilidad
     * @throws SQLException Si ocurre error en la consulta
     */
    public List<resultadosBusqueda> buscarVentasParaDevolucion(String criterio) throws SQLException {
        // VALIDACIÓN DE ENTRADA
        if (criterio == null || criterio.trim().isEmpty()) {
            throw new IllegalArgumentException("El criterio de búsqueda no puede estar vacío");
        }

        List<resultadosBusqueda> resultados = new ArrayList<>();
        Connection con = null;

        try {
            con = conexion.getInstance().createConnection();

            // CONSULTA OPTIMIZADA CON JOINS PARA MEJOR PERFORMANCE
            String sql = construirConsultaBusqueda();

            try (PreparedStatement ps = con.prepareStatement(sql)) {
                // CONFIGURAR PARÁMETROS DE BÚSQUEDA
                configurarParametrosBusqueda(ps, criterio);

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        resultadosBusqueda resultado = mapearResultadoDesdeRS(rs);
                        if (resultado != null) {
                            resultados.add(resultado);
                        }
                    }
                }
            }

            // POST-PROCESAMIENTO: Cargar detalles adicionales si es necesario
            for (resultadosBusqueda resultado : resultados) {
                enriquecerResultadoConDetalles(resultado, con);
            }

            LOGGER.info(String.format("Búsqueda completada: %d resultados para criterio '%s'",
                    resultados.size(), criterio));

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error en búsqueda de ventas: " + e.getMessage(), e);
            throw new SQLException("Error realizando búsqueda: " + e.getMessage(), e);
        } finally {
            if (con != null) {
                try {
                    con.close();
                } catch (SQLException ex) {
                    LOGGER.log(Level.WARNING, "Error cerrando conexión", ex);
                }
            }
        }

        return resultados;
    }

    /**
     * Busca venta específica por ID con toda la información necesaria
     *
     * @param idVenta ID de la venta a buscar
     * @return Resultado detallado de la venta o null si no existe
     * @throws SQLException Si ocurre error en la consulta
     */
    public resultadosBusqueda buscarVentaPorId(int idVenta) throws SQLException {
        try {
            // REUTILIZAR LÓGICA EXISTENTE DEL ServiceVenta
            ModelVenta venta = serviceVenta.buscarVentaPorId(idVenta);

            if (venta != null) {
                return crearResultadoDesdeModelVenta(venta);
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error buscando venta por ID: " + idVenta, e);
            throw e;
        }

        return null;
    }

    // ====================================================================
    // MÉTODOS AUXILIARES DE CONSTRUCCIÓN DE CONSULTAS
    // ====================================================================
    /**
     * Construye la consulta SQL optimizada para búsqueda PRINCIPIO OCP:
     * Fácilmente extensible para nuevos campos
     */
    private String construirConsultaBusqueda() {
        return " SELECT DISTINCT \n"
                + "                v.id_venta,\n"
                + "                v.fecha_venta,\n"
                + "                v.subtotal,\n"
                + "                v.descuento,\n"
                + "                v.iva,\n"
                + "                v.total,\n"
                + "                v.estado,\n"
                + "                v.tipo_pago,\n"
                + "                v.observaciones,\n"
                + "                -- Información del cliente\n"
                + "                c.id_cliente,\n"
                + "                c.nombre as cliente_nombre,\n"
                + "                c.dni as cliente_dni,\n"
                + "                c.telefono as cliente_telefono,\n"
                + "                -- Información del usuario vendedor\n"
                + "                u.id_usuario,\n"
                + "                u.nombre as usuario_nombre,\n"
                + "                -- Información de la caja\n"
                + "                cj.id_caja,\n"
                + "                cj.nombre as caja_nombre,\n"
                + "                -- Información del movimiento\n"
                + "                cm.id_movimiento,\n"
                + "                -- Cálculo de días transcurridos\n"
                + "                DATEDIFF(CURDATE(), v.fecha_venta) as dias_transcurridos,\n"
                + "                -- Conteo de productos\n"
                + "                COUNT(DISTINCT vd.id_detalle) as total_productos,\n"
                + "                SUM(vd.cantidad) as total_items\n"
                + "            FROM ventas v\n"
                + "            LEFT JOIN clientes c ON v.id_cliente = c.id_cliente\n"
                + "            INNER JOIN usuarios u ON v.id_usuario = u.id_usuario\n"
                + "            INNER JOIN cajas cj ON v.id_caja = cj.id_caja\n"
                + "            INNER JOIN caja_movimientos cm ON v.id_movimiento = cm.id_movimiento\n"
                + "            LEFT JOIN venta_detalles vd ON v.id_venta = vd.id_venta AND vd.activo = 1\n"
                + "            LEFT JOIN productos p ON vd.id_producto = p.id_producto\n"
                + "            LEFT JOIN producto_variantes pv ON vd.id_variante = pv.id_variante\n"
                + "            WHERE v.estado = 'completada'\n"
                + "            AND (\n"
                + "                v.id_venta = ? OR\n"
                + "                c.dni LIKE ? OR\n"
                + "                c.nombre LIKE ? OR\n"
                + "                c.telefono LIKE ? OR\n"
                + "                p.codigo_modelo LIKE ? OR\n"
                + "                p.nombre LIKE ? OR\n"
                + "                pv.sku LIKE ? OR\n"
                + "                pv.ean LIKE ?\n"
                + "            )\n"
                + "            GROUP BY v.id_venta\n"
                + "            ORDER BY v.fecha_venta DESC\n"
                + "            LIMIT ?";
    }

    /**
     * Configura los parámetros de la consulta de búsqueda
     */
    private void configurarParametrosBusqueda(PreparedStatement ps, String criterio) throws SQLException {
        String criterioBusqueda = "%" + criterio.trim() + "%";

        // Intentar parsear como ID numérico
        Integer idVenta = null;
        try {
            idVenta = Integer.parseInt(criterio.trim());
        } catch (NumberFormatException e) {
            idVenta = 0; // Si no es número, usar 0
        }

        // CONFIGURAR TODOS LOS PARÁMETROS
        ps.setInt(1, idVenta);                    // ID venta exacto
        ps.setString(2, criterioBusqueda);        // DNI cliente
        ps.setString(3, criterioBusqueda);        // Nombre cliente
        ps.setString(4, criterioBusqueda);        // Teléfono cliente
        ps.setString(5, criterioBusqueda);        // Código modelo producto
        ps.setString(6, criterioBusqueda);        // Nombre producto
        ps.setString(7, criterioBusqueda);        // SKU variante
        ps.setString(8, criterioBusqueda);        // EAN variante
        ps.setInt(9, LIMITE_RESULTADOS);          // Límite de resultados
    }

    // ====================================================================
    // MÉTODOS DE MAPEO DE DATOS
    // ====================================================================
    /**
     * Mapea un ResultSet a ResultadoBusquedaVenta APLICANDO CLEAN CODE: Método
     * único con responsabilidad específica
     */
    private resultadosBusqueda mapearResultadoDesdeRS(ResultSet rs) throws SQLException {
        try {
            // MAPEAR DATOS BÁSICOS DE LA VENTA
            int idVenta = rs.getInt("id_venta");
            LocalDateTime fechaVenta = rs.getTimestamp("fecha_venta").toLocalDateTime();
            double total = rs.getDouble("total");
            String estado = rs.getString("estado");
            String tipoPago = rs.getString("tipo_pago");

            // MAPEAR INFORMACIÓN DEL CLIENTE
            Integer idCliente = rs.getObject("id_cliente") != null ? rs.getInt("id_cliente") : null;
            String clienteNombre = rs.getString("cliente_nombre");
            String clienteDni = rs.getString("cliente_dni");
            String clienteTelefono = rs.getString("cliente_telefono");

            // MAPEAR INFORMACIÓN DEL VENDEDOR
            int idUsuario = rs.getInt("id_usuario");
            String usuarioNombre = rs.getString("usuario_nombre");

            // CALCULAR ELEGIBILIDAD PARA DEVOLUCIÓN
            int diasTranscurridos = rs.getInt("dias_transcurridos");
            boolean elegibleDevolucion = diasTranscurridos <= DIAS_LIMITE_DEVOLUCION;

            // INFORMACIÓN ADICIONAL
            int totalProductos = rs.getInt("total_productos");
            int totalItems = rs.getInt("total_items");

            // CREAR Y RETORNAR RESULTADO
            return new resultadosBusqueda(
                    idVenta, fechaVenta, total, estado, tipoPago,
                    idCliente, clienteNombre, clienteDni, clienteTelefono,
                    idUsuario, usuarioNombre,
                    diasTranscurridos, elegibleDevolucion,
                    totalProductos, totalItems
            );

        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error mapeando resultado de búsqueda", e);
            return null;
        }
    }

    /**
     * Crea ResultadoBusquedaVenta desde ModelVenta existente REUTILIZACIÓN:
     * Aprovecha modelos existentes
     */
    private resultadosBusqueda crearResultadoDesdeModelVenta(ModelVenta venta) {
        // CALCULAR DÍAS TRANSCURRIDOS
        long diasTranscurridos = java.time.temporal.ChronoUnit.DAYS.between(
                venta.getFechaVenta().toLocalDate(),
                java.time.LocalDate.now()
        );

        boolean elegibleDevolucion = diasTranscurridos <= DIAS_LIMITE_DEVOLUCION;

        // EXTRAER INFORMACIÓN DEL CLIENTE
        Integer idCliente = null;
        String clienteNombre = "Cliente General";
        String clienteDni = null;
        String clienteTelefono = null;

        if (venta.getCliente() != null) {
            idCliente = venta.getCliente().getIdCliente();
            clienteNombre = venta.getCliente().getNombre();
            clienteDni = venta.getCliente().getDni();
            clienteTelefono = venta.getCliente().getTelefono();
        }

        // CONTAR PRODUCTOS
        int totalProductos = venta.getDetalles() != null ? venta.getDetalles().size() : 0;
        int totalItems = venta.getDetalles() != null
                ? venta.getDetalles().stream().mapToInt(d -> d.getCantidad()).sum() : 0;

        return new resultadosBusqueda(
                venta.getIdVenta(),
                venta.getFechaVenta(),
                venta.getTotal(),
                venta.getEstado(),
                venta.getTipoPago(),
                idCliente,
                clienteNombre,
                clienteDni,
                clienteTelefono,
                venta.getUsuario().getIdUsuario(),
                venta.getUsuario().getNombre(),
                (int) diasTranscurridos,
                elegibleDevolucion,
                totalProductos,
                totalItems
        );
    }

    /**
     * Enriquece el resultado con detalles adicionales si es necesario PRINCIPIO
     * OCP: Extensible para más enriquecimientos
     */
    private void enriquecerResultadoConDetalles(resultadosBusqueda resultado, Connection con) {
        // IMPLEMENTAR SI SE NECESITAN DETALLES ADICIONALES
        // Por ejemplo: productos específicos, métodos de pago, etc.

        try {
            // Ejemplo: Cargar método de pago específico si es mixto
            if ("mixto".equals(resultado.getTipoPago())) {
                cargarDetallesPagoMixto(resultado, con);
            }

        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error enriqueciendo resultado ID: " + resultado.getIdVenta(), e);
            // No lanzar excepción, solo loggear el error
        }
    }

    /**
     * Carga detalles de pago mixto (ejemplo de extensibilidad)
     */
    private void cargarDetallesPagoMixto(resultadosBusqueda resultado, Connection con) throws SQLException {
        String sql = "  SELECT tipo_pago, monto, referencia \n"
                + "            FROM venta_pagos \n"
                + "            WHERE id_venta = ? AND estado = 'aprobado'\n"
                + "            ORDER BY fecha_pago";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, resultado.getIdVenta());

            try (ResultSet rs = ps.executeQuery()) {
                StringBuilder detallesPago = new StringBuilder();
                while (rs.next()) {
                    if (detallesPago.length() > 0) {
                        detallesPago.append(", ");
                    }
                    detallesPago.append(rs.getString("tipo_pago"))
                            .append(": $")
                            .append(String.format("%.2f", rs.getDouble("monto")));
                }

                // Establecer en resultado si tiene método para ello
                // resultado.setDetallesPago(detallesPago.toString());
            }
        }
    }

    public ValidacionElegibilidad validarElegibilidadDevolucion(int idVenta) throws SQLException {
        String sql = "SELECT \n" +
"                v.estado,\n" +
"                v.fecha_venta,\n" +
"                DATEDIFF(CURDATE(), v.fecha_venta) as dias_transcurridos,\n" +
"                COUNT(d.id_devolucion) as devoluciones_existentes,\n" +
"                v.total as total_original\n" +
"            FROM ventas v\n" +
"            LEFT JOIN devoluciones d ON v.id_venta = d.id_venta AND d.activa = 1\n" +
"            WHERE v.id_venta = ?\n" +
"            GROUP BY v.id_venta";

        try (Connection con = conexion.getInstance().createConnection(); PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idVenta);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String estado = rs.getString("estado");
                    int diasTranscurridos = rs.getInt("dias_transcurridos");
                    int devolucionesExistentes = rs.getInt("devoluciones_existentes");
                    double totalOriginal = rs.getDouble("total_original");

                    // APLICAR REGLAS DE NEGOCIO
                    return new ValidacionElegibilidad(
                            "completada".equals(estado),
                            diasTranscurridos <= DIAS_LIMITE_DEVOLUCION,
                            devolucionesExistentes == 0,
                            totalOriginal > 0,
                            diasTranscurridos,
                            generarMensajeValidacion(estado, diasTranscurridos, devolucionesExistentes)
                    );
                }
            }
        }

        return new ValidacionElegibilidad(false, false, false, false, -1, "Venta no encontrada");
    }

    /**
     * Genera mensaje descriptivo de validación
     */
    private String generarMensajeValidacion(String estado, int diasTranscurridos, int devolucionesExistentes) {
        if (!"completada".equals(estado)) {
            return "La venta debe estar en estado 'completada'";
        }
        if (diasTranscurridos > DIAS_LIMITE_DEVOLUCION) {
            return String.format("Han transcurrido %d días. Máximo permitido: %d días",
                    diasTranscurridos, DIAS_LIMITE_DEVOLUCION);
        }
        if (devolucionesExistentes > 0) {
            return "Esta venta ya tiene devoluciones procesadas";
        }
        return "Venta elegible para devolución";
    }
}
