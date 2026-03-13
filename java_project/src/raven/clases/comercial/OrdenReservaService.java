package raven.clases.comercial;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.time.LocalDateTime;
import raven.dao.OrdenReservaDAO;
import raven.modelos.OrdenReserva;
import raven.modelos.OrdenReservaDetalle;

import raven.clases.admin.UserSession;
import raven.controlador.admin.ModelUser;
import raven.controlador.admin.ModelCaja;
import raven.controlador.principal.ModelVenta;
import raven.controlador.principal.ModelDetalleVenta;
import raven.controlador.productos.ModelProduct;
import raven.clases.principal.ServiceVenta;

/**
 * Servicio para manejar la lógica de negocio de las órdenes de reserva.
 */
public class OrdenReservaService {
    
    private final OrdenReservaDAO ordenReservaDAO;
    
    public OrdenReservaService() {
        this.ordenReservaDAO = new OrdenReservaDAO();
    }
    
    /**
     * Obtiene todas las órdenes de reserva.
     * @return Lista de órdenes de reserva
     * @throws SQLException Si ocurre un error de base de datos
     */
    public List<OrdenReserva> obtenerTodasLasOrdenes() throws SQLException {
        return obtenerTodasLasOrdenes(null);
    }

    /**
     * Obtiene todas las órdenes de reserva filtradas por bodega.
     * @param idBodega ID de la bodega (null para todas)
     * @return Lista de órdenes de reserva
     * @throws SQLException Si ocurre un error de base de datos
     */
    public List<OrdenReserva> obtenerTodasLasOrdenes(Integer idBodega) throws SQLException {
        return ordenReservaDAO.obtenerTodasLasOrdenes(idBodega);
    }
    
    /**
     * Obtiene todas las órdenes de reserva como mapas para la tabla.
     * @return Lista de mapas con datos de las órdenes
     * @throws SQLException Si ocurre un error de base de datos
     */
    public List<Map<String, Object>> obtenerTodasLasOrdenesParaTabla() throws SQLException {
        return obtenerTodasLasOrdenesParaTabla(null);
    }

    /**
     * Obtiene todas las órdenes de reserva como mapas para la tabla, filtradas por bodega.
     * @param idBodega ID de la bodega (null para todas)
     * @return Lista de mapas con datos de las órdenes
     * @throws SQLException Si ocurre un error de base de datos
     */
    public List<Map<String, Object>> obtenerTodasLasOrdenesParaTabla(Integer idBodega) throws SQLException {
        List<OrdenReserva> ordenes = ordenReservaDAO.obtenerTodasLasOrdenes(idBodega);
        List<Map<String, Object>> resultado = new ArrayList<>();
        
        for (OrdenReserva orden : ordenes) {
            Map<String, Object> mapa = new HashMap<>();
            mapa.put("id", "OR-" + orden.getIdOrden());
            mapa.put("fecha", orden.getFechaCreacion());
            mapa.put("cliente", orden.getNombreUsuario() != null ? orden.getNombreUsuario().trim() : "Usuario " + orden.getIdUsuario());
            mapa.put("bodega", orden.getNombreBodega() != null ? orden.getNombreBodega().trim() : "Sin bodega");
            
            String estado = orden.getEstado() != null ? orden.getEstado().trim() : "";
            if (estado.equalsIgnoreCase("pendiente productos")) {
                estado = "pendiente";
            }
            mapa.put("estado", estado);
            
            mapa.put("productos", orden.getCantidadProductos());
            mapa.put("fecha_retirado", orden.getFechaRetirado());
            mapa.put("fecha_pagado", orden.getFechaPagado());
            mapa.put("fecha_finalizado", orden.getFechaFinalizado());
            resultado.add(mapa);
        }
        
        return resultado;
    }
    
    /**
     * Obtiene las órdenes de reserva filtradas por estado.
     * @param estado Estado de la orden (PENDIENTE, RETIRADO, PAGADO, FINALIZADO)
     * @return Lista de órdenes de reserva
     * @throws SQLException Si ocurre un error de base de datos
     */
    public List<OrdenReserva> obtenerOrdenesPorEstado(String estado) throws SQLException {
        return ordenReservaDAO.obtenerOrdenesPorEstado(estado);
    }
    
    /**
     * Obtiene las órdenes pendientes (estado PENDIENTE).
     * @return Lista de órdenes pendientes
     * @throws SQLException Si ocurre un error de base de datos
     */
    public List<OrdenReserva> obtenerOrdenesPendientes() throws SQLException {
        return ordenReservaDAO.obtenerOrdenesPorEstado("PENDIENTE");
    }
    
    /**
     * Obtiene una orden de reserva por su ID.
     * @param idOrden ID de la orden
     * @return Orden de reserva o null si no existe
     * @throws SQLException Si ocurre un error de base de datos
     */
    public OrdenReserva obtenerOrdenPorId(int idOrden) throws SQLException {
        return ordenReservaDAO.obtenerOrdenPorId(idOrden);
    }
    
    /**
     * Obtiene los detalles de una orden de reserva.
     * @param idOrden ID de la orden
     * @return Lista de detalles de la orden
     * @throws SQLException Si ocurre un error de base de datos
     */
    public List<OrdenReservaDetalle> obtenerDetallesOrden(int idOrden) throws SQLException {
        return ordenReservaDAO.obtenerDetallesOrden(idOrden);
    }
    
    /**
     * Cambia el estado de una orden de reserva de PENDIENTE a RETIRADO.
     * @param idOrden ID de la orden
     * @return true si se actualizó correctamente, false en caso contrario
     * @throws SQLException Si ocurre un error de base de datos
     */
    public boolean marcarComoRetirado(int idOrden) throws SQLException {
        // Verificar que la orden existe y está en estado PENDIENTE
        OrdenReserva orden = ordenReservaDAO.obtenerOrdenPorId(idOrden);
        if (orden == null) {
            throw new IllegalArgumentException("La orden con ID " + idOrden + " no existe");
        }
        
        if (!"pendiente".equals(normalizarEstadoDb(orden.getEstado()))) {
            throw new IllegalStateException("La orden debe estar en estado PENDIENTE para poder marcarla como RETIRADO");
        }
        
        return ordenReservaDAO.actualizarEstadoOrden(idOrden, "retirado");
    }
    
    /**
     * Cambia el estado de una orden de reserva de RETIRADO a PAGADO.
     * @param idOrden ID de la orden
     * @return true si se actualizó correctamente, false en caso contrario
     * @throws SQLException Si ocurre un error de base de datos
     */
    public boolean marcarComoPagado(int idOrden) throws SQLException {
        // Verificar que la orden existe y está en estado RETIRADO
        OrdenReserva orden = ordenReservaDAO.obtenerOrdenPorId(idOrden);
        if (orden == null) {
            throw new IllegalArgumentException("La orden con ID " + idOrden + " no existe");
        }
        
        if (!"retirado".equals(normalizarEstadoDb(orden.getEstado()))) {
            throw new IllegalStateException("La orden debe estar en estado RETIRADO para poder marcarla como PAGADO");
        }
        
        return ordenReservaDAO.actualizarEstadoOrden(idOrden, "pagado");
    }
    
    /**
     * Cambia el estado de una orden de reserva de PAGADO a FINALIZADO.
     * @param idOrden ID de la orden
     * @return true si se actualizó correctamente, false en caso contrario
     * @throws SQLException Si ocurre un error de base de datos
     */
    public boolean marcarComoFinalizado(int idOrden) throws SQLException {
        // Verificar que la orden existe y está en estado PAGADO
        OrdenReserva orden = ordenReservaDAO.obtenerOrdenPorId(idOrden);
        if (orden == null) {
            throw new IllegalArgumentException("La orden con ID " + idOrden + " no existe");
        }
        
        if (!"pagado".equals(normalizarEstadoDb(orden.getEstado()))) {
            throw new IllegalStateException("La orden debe estar en estado PAGADO para poder marcarla como FINALIZADO");
        }
        
        return ordenReservaDAO.actualizarEstadoOrden(idOrden, "finalizado");
    }
    
    /**
     * Obtiene estadísticas de órdenes para un rango de fechas y bodega opcional.
     * @param inicio Fecha inicio
     * @param fin Fecha fin
     * @param idBodega ID de la bodega (null para todas)
     * @return Mapa con estadísticas
     * @throws SQLException Si ocurre un error de base de datos
     */
    public Map<String, Object> obtenerEstadisticasOrdenes(java.time.LocalDate inicio, java.time.LocalDate fin, Integer idBodega) throws SQLException {
        return ordenReservaDAO.obtenerEstadisticasOrdenes(inicio, fin, idBodega);
    }

    /**
     * Filtra las órdenes de reserva según múltiples criterios.
     * @param fechaInicio Fecha inicio del rango (opcional)
     * @param fechaFin Fecha fin del rango (opcional)
     * @param idUsuario ID del usuario/cliente (opcional, null o <= 0 para ignorar)
     * @param estado Estado de la orden (opcional, null o vacío para ignorar)
     * @param idBodega ID de la bodega (opcional, null para ignorar)
     * @return Lista de mapas con datos de las órdenes filtradas
     * @throws SQLException Si ocurre un error de base de datos
     */
    public List<Map<String, Object>> filtrarOrdenes(java.util.Date fechaInicio, java.util.Date fechaFin, Integer idUsuario, String estado, Integer idBodega) throws SQLException {
        List<OrdenReserva> ordenes = ordenReservaDAO.filtrarOrdenes(fechaInicio, fechaFin, idUsuario, estado, idBodega);
        List<Map<String, Object>> resultado = new ArrayList<>();
        
        for (OrdenReserva orden : ordenes) {
            Map<String, Object> mapa = new HashMap<>();
            mapa.put("id", "OR-" + orden.getIdOrden());
            mapa.put("fecha", orden.getFechaCreacion());
            mapa.put("cliente", orden.getNombreUsuario() != null ? orden.getNombreUsuario().trim() : "Usuario " + orden.getIdUsuario());
            mapa.put("bodega", orden.getNombreBodega() != null ? orden.getNombreBodega().trim() : "Sin bodega");
            
            String estadoStr = orden.getEstado() != null ? orden.getEstado().trim() : "";
            if (estadoStr.equalsIgnoreCase("pendiente productos")) {
                estadoStr = "pendiente";
            }
            mapa.put("estado", estadoStr);
            
            mapa.put("productos", orden.getCantidadProductos());
            mapa.put("fecha_retirado", orden.getFechaRetirado());
            mapa.put("fecha_pagado", orden.getFechaPagado());
            mapa.put("fecha_finalizado", orden.getFechaFinalizado());
            resultado.add(mapa);
        }
        
        return resultado;
    }

    /**
     * Procesa una orden de reserva para convertirla en venta.
     * Este método prepara los datos de la orden para ser procesados en el carrito de ventas.
     * @param idOrden ID de la orden a procesar
     * @return Lista de detalles de la orden listos para agregar al carrito
     * @throws SQLException Si ocurre un error de base de datos
     */
    public List<OrdenReservaDetalle> procesarOrdenParaVenta(int idOrden) throws SQLException {
        // Verificar que la orden existe y está en estado PENDIENTE
        OrdenReserva orden = ordenReservaDAO.obtenerOrdenPorId(idOrden);
        if (orden == null) {
            throw new IllegalArgumentException("La orden con ID " + idOrden + " no existe");
        }
        
        if (!"pendiente".equals(normalizarEstadoDb(orden.getEstado()))) {
            throw new IllegalStateException("Solo se pueden procesar órdenes en estado PENDIENTE");
        }
        
        // Obtener los detalles de la orden
        List<OrdenReservaDetalle> detalles = ordenReservaDAO.obtenerDetallesOrden(idOrden);
        
        if (detalles.isEmpty()) {
            throw new IllegalStateException("La orden no tiene productos asociados");
        }
        
        return detalles;
    }
    
    /**
     * Convierte una orden de reserva en venta.
     * @param idOrden ID de la orden a convertir
     * @return ID de la venta creada, o -1 en caso de error
     * @throws SQLException Si ocurre un error de base de datos
     */
    public int convertirOrdenAVenta(int idOrden) throws SQLException {
        try {
            // Obtener y validar la orden
            OrdenReserva orden = ordenReservaDAO.obtenerOrdenPorId(idOrden);
            if (orden == null) {
                throw new IllegalArgumentException("La orden con ID " + idOrden + " no existe");
            }
            String estadoOrden = normalizarEstadoDb(orden.getEstado());
            if (!"pendiente".equals(estadoOrden) &&
                !"retirado".equals(estadoOrden) &&
                !"pagado".equals(estadoOrden)) {
                throw new IllegalStateException("La orden debe estar en estado PENDIENTE/RETIRADO/PAGADO para convertirla");
            }

            // Obtener detalles de la orden
            List<OrdenReservaDetalle> detallesOrden = ordenReservaDAO.obtenerDetallesOrden(idOrden);
            if (detallesOrden == null || detallesOrden.isEmpty()) {
                throw new IllegalStateException("La orden no tiene productos asociados");
            }

            // Obtener sesión de usuario y caja activa
            UserSession session = UserSession.getInstance();
            ModelUser usuario = session.getCurrentUser();
            Integer idCaja = session.getIdCajaAsociada();
            if (usuario == null || idCaja == null) {
                throw new IllegalStateException("No hay usuario o caja activa en la sesión");
            }

            // Construir la venta
            ModelVenta venta = new ModelVenta();
            venta.setUsuario(usuario);
            ModelCaja caja = new ModelCaja();
            caja.setIdCaja(idCaja);
            venta.setCaja(caja);
            venta.setFechaVenta(LocalDateTime.now());

            double subtotal = 0.0;
            List<ModelDetalleVenta> detallesVenta = new ArrayList<>();
            for (OrdenReservaDetalle d : detallesOrden) {
                subtotal += d.getSubtotal();
                ModelDetalleVenta dv = new ModelDetalleVenta();
                ModelProduct producto = new ModelProduct();
                producto.setProductId(d.getIdProducto());
                dv.setProducto(producto);
                dv.setIdVariante(d.getIdVariante());
                dv.setCantidad(d.getCantidad());
                dv.setPrecioUnitario(d.getPrecio());
                dv.setDescuento(0);
                dv.setSubtotal(d.getSubtotal());
                // Asumimos venta por pares
                dv.setTipoVenta("salida par");
                detallesVenta.add(dv);
            }
            venta.setDetalles(detallesVenta);

            venta.setSubtotal(subtotal);
            venta.setDescuento(0);
            venta.setIva(0);
            venta.setTotal(subtotal);
            venta.setEstado("completada");
            venta.setTipoPago("efectivo");
            venta.setObservaciones("Generada desde orden de reserva OR-" + idOrden);

            // Crear la venta (maneja inventario y caja internamente)
            ServiceVenta serviceVenta = new ServiceVenta();
            serviceVenta.crearVenta(venta);

            // Marcar la orden como FINALIZADO
            if (!"finalizado".equals(estadoOrden)) {
                ordenReservaDAO.actualizarEstadoOrden(idOrden, "finalizado");
            }

            return venta.getIdVenta();
        } catch (Exception e) {
            return -1;
        }
    }
    
    /**
     * Actualiza el estado de una orden de reserva.
     * @param idOrden ID de la orden
     * @param nuevoEstado Nuevo estado
     * @return true si se actualizó correctamente
     * @throws SQLException Si ocurre un error de base de datos
     */
    public boolean actualizarEstadoOrden(int idOrden, String nuevoEstado) throws SQLException {
        return ordenReservaDAO.actualizarEstadoOrden(idOrden, normalizarEstadoDb(nuevoEstado));
    }
    
    /**
     * Cambia el estado de una orden de reserva (alias para actualizarEstadoOrden).
     * @param idOrden ID de la orden
     * @param nuevoEstado Nuevo estado
     * @return true si se actualizó correctamente
     * @throws SQLException Si ocurre un error de base de datos
     */
    public boolean cambiarEstadoOrden(int idOrden, String nuevoEstado) throws SQLException {
        return actualizarEstadoOrden(idOrden, nuevoEstado);
    }
    
    /**
     * Valida si una orden puede cambiar de estado.
     * @param idOrden ID de la orden
     * @param nuevoEstado Nuevo estado deseado
     * @return true si el cambio es válido, false en caso contrario
     * @throws SQLException Si ocurre un error de base de datos
     */
    public boolean validarCambioEstado(int idOrden, String nuevoEstado) throws SQLException {
        OrdenReserva orden = ordenReservaDAO.obtenerOrdenPorId(idOrden);
        if (orden == null) {
            return false;
        }
        
        String estadoActual = normalizarEstadoDb(orden.getEstado());
        String nuevoEstadoNorm = normalizarEstadoDb(nuevoEstado);
        
        // Definir las transiciones válidas de estado
        switch (estadoActual) {
            case "pendiente":
                return "retirado".equals(nuevoEstadoNorm);
            case "retirado":
                return "pagado".equals(nuevoEstadoNorm);
            case "pagado":
                return "finalizado".equals(nuevoEstadoNorm);
            case "finalizado":
                return false; // No se puede cambiar desde FINALIZADO
            default:
                return false;
        }
    }

    private String normalizarEstadoDb(String estado) {
        if (estado == null) {
            return "";
        }
        String e = estado.trim().replaceAll("\\s+", " ").toLowerCase();
        if (e.endsWith(" productos")) {
            e = e.substring(0, e.length() - " productos".length()).trim();
        }
        if ("cancelar".equals(e)) {
            e = "cancelada";
        }
        return e;
    }
}
