package raven.clases.productos;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import raven.dao.PrestamoZapatoDAO;
import raven.modelos.PrestamoZapato;
import raven.clases.productos.ServiceProductVariant;

/**
    * Servicio de negocio para gestionar préstamos de zapatos.
    * Expone operaciones de alto nivel para UI/controladores.
 */
public class PrestamoZapatoService {

    private final PrestamoZapatoDAO dao;

    public PrestamoZapatoService() {
        this.dao = new PrestamoZapatoDAO();
    }

    // Crear
    public int crearPrestamo(PrestamoZapato p) throws SQLException {
        if (p.getEstado() == null || p.getEstado().isBlank()) {
            p.setEstado(PrestamoZapatoDAO.ESTADO_PRESTADO);
        }
        if (p.getFechaPrestamo() == null) {
            p.setFechaPrestamo(new Timestamp(System.currentTimeMillis()));
        }
        // 1) Insertar préstamo
        int idGenerado = dao.insertarPrestamo(p);

        // 2) Tras insertar, ajustar stock de pares si el préstamo consume un par para venta
        try {
            ajustarStockPorPrestamoCreado(p);
        } catch (Exception ex) {
            // No romper creación por error de ajuste; solo registrar
            System.err.println("Advertencia: no se pudo ajustar stock tras crear préstamo: " + ex.getMessage());
        }

        return idGenerado;
    }

    // Ver por ID
    public PrestamoZapato obtenerPrestamo(int idPrestamo) throws SQLException {
        return dao.obtenerPrestamoPorId(idPrestamo);
    }

    // Listar (filtros opcionales)
    public List<PrestamoZapato> listarPrestamos(String estado, Integer idBodega, Integer idProducto, Integer idVariante) throws SQLException {
        return dao.listarPrestamos(estado, idBodega, idProducto, idVariante);
    }

    // Actualizar datos principales
    public boolean actualizarDatosPrestamo(PrestamoZapato p) throws SQLException {
        return dao.actualizarDatosPrestamo(p);
    }

    // Marcar devolución
    public boolean devolverPrestamo(int idPrestamo, String observaciones, Integer idUsuario) throws SQLException {
        // Obtener datos del préstamo antes de devolver para calcular consumo
        PrestamoZapato prestamo = dao.obtenerPrestamoPorId(idPrestamo);
        if (prestamo == null) {
            throw new SQLException("Préstamo no encontrado: " + idPrestamo);
        }

        // Reglas de estado: si ya está DEVUELTO, PERDIDO o DAÑADO, no permitir marcar DEVUELTO
        String estadoActual = prestamo.getEstado() != null ? prestamo.getEstado() : "";
        if (PrestamoZapatoDAO.ESTADO_DEVUELTO.equalsIgnoreCase(estadoActual)) {
            throw new SQLException("Operación inválida: el préstamo ya está DEVUELTO.");
        }
        if (PrestamoZapatoDAO.ESTADO_PERDIDO.equalsIgnoreCase(estadoActual)
                || PrestamoZapatoDAO.ESTADO_DANADO.equalsIgnoreCase(estadoActual)) {
            throw new SQLException("Operación inválida: no se puede cambiar de PERDIDO/DAÑADO a DEVUELTO.");
        }

        boolean actualizado = dao.marcarDevuelto(idPrestamo, observaciones, idUsuario);

        if (actualizado) {
            try {
                ajustarStockPorPrestamoDevuelto(prestamo);
            } catch (Exception ex) {
                System.err.println("Advertencia: no se pudo reponer stock tras devolver préstamo: " + ex.getMessage());
            }
        }
        return actualizado;
    }

    // Lista estados disponibles desde BD
    public List<String> listarEstadosDisponibles() throws SQLException {
        return dao.listarEstadosDisponibles();
    }

    // Eliminar préstamo con reglas de inventario
    public boolean eliminarPrestamo(int idPrestamo, Integer idUsuario) throws SQLException {
        PrestamoZapato prestamo = dao.obtenerPrestamoPorId(idPrestamo);
        if (prestamo == null) {
            throw new SQLException("Préstamo no encontrado: " + idPrestamo);
        }

        // Si está PRESTADO y el préstamo consumió un par, reponer stock
        String estadoActual = prestamo.getEstado() != null ? prestamo.getEstado().trim().toUpperCase() : "";
        if (PrestamoZapatoDAO.ESTADO_PRESTADO.equalsIgnoreCase(estadoActual)) {
            try {
                if (consumioParAlCrear(prestamo)) {
                    ServiceProductVariant spv = new ServiceProductVariant();
                    spv.addPairsStock(prestamo.getIdVariante(), 1);
                }
            } catch (Exception ex) {
                System.err.println("Advertencia: no se pudo reponer stock al eliminar préstamo PRESTADO: " + ex.getMessage());
            }
        }

        // Eliminar físicamente
        return dao.eliminarPrestamo(idPrestamo);
    }

    // Determina si al crear el préstamo se descontó un par del stock
    private boolean consumioParAlCrear(PrestamoZapato p) {
        if (p == null || p.getIdVariante() == null || p.getIdVariante() <= 0) return false;
        String pie = p.getPie() != null ? p.getPie().trim().toUpperCase() : "";
        if ("AMBOS".equals(pie)) return true; // siempre consumió 1 par
        if ("DERECHO".equals(pie) || "IZQUIERDO".equals(pie)) {
            // si fue un pie nuevo, consumió 1 par
            boolean esNuevo = p.isNuevoPar();
            if (!esNuevo) {
                String obs = p.getObservaciones();
                esNuevo = obs != null && obs.contains("[NUEVO]");
            }
            return esNuevo;
        }
        return false;
    }

    // Cambiar estado de préstamo y ajustar inventario según transición
    public boolean cambiarEstadoPrestamo(int idPrestamo, String nuevoEstado) throws SQLException {
        PrestamoZapato prestamo = dao.obtenerPrestamoPorId(idPrestamo);
        if (prestamo == null) {
            throw new SQLException("Préstamo no encontrado: " + idPrestamo);
        }

        String estadoAnterior = prestamo.getEstado() != null ? prestamo.getEstado() : "";
        // Reglas de estado
        // 1) Si está DEVUELTO, no se puede cambiar a ningún otro estado
        if (PrestamoZapatoDAO.ESTADO_DEVUELTO.equalsIgnoreCase(estadoAnterior)
                && !PrestamoZapatoDAO.ESTADO_DEVUELTO.equalsIgnoreCase(nuevoEstado)) {
            throw new SQLException("Operación inválida: un préstamo DEVUELTO no puede cambiar de estado.");
        }
        // 2) Si está PERDIDO o DAÑADO, no permitir cambiar a PRESTADO ni DEVUELTO
        if ((PrestamoZapatoDAO.ESTADO_PERDIDO.equalsIgnoreCase(estadoAnterior)
                || PrestamoZapatoDAO.ESTADO_DANADO.equalsIgnoreCase(estadoAnterior))
                && (PrestamoZapatoDAO.ESTADO_PRESTADO.equalsIgnoreCase(nuevoEstado)
                || PrestamoZapatoDAO.ESTADO_DEVUELTO.equalsIgnoreCase(nuevoEstado))) {
            throw new SQLException("Operación inválida: no se puede cambiar de PERDIDO/DAÑADO a PRESTADO/DEVUELTO.");
        }
        Timestamp fechaDevolucion = null;
        if (PrestamoZapatoDAO.ESTADO_DEVUELTO.equalsIgnoreCase(nuevoEstado)) {
            fechaDevolucion = new Timestamp(System.currentTimeMillis());
        }

        boolean ok = dao.actualizarEstadoPrestamo(idPrestamo, nuevoEstado, fechaDevolucion, null, prestamo.getIdUsuario());
        if (ok) {
            try {
                boolean aDevuelto = PrestamoZapatoDAO.ESTADO_PRESTADO.equalsIgnoreCase(estadoAnterior)
                        && PrestamoZapatoDAO.ESTADO_DEVUELTO.equalsIgnoreCase(nuevoEstado);
                boolean aPrestado = PrestamoZapatoDAO.ESTADO_DEVUELTO.equalsIgnoreCase(estadoAnterior)
                        && PrestamoZapatoDAO.ESTADO_PRESTADO.equalsIgnoreCase(nuevoEstado);

                if (aDevuelto) {
                    ajustarStockPorPrestamoDevuelto(prestamo);
                } else if (aPrestado) {
                    ajustarStockPorPrestamoCreado(prestamo);
                }
            } catch (Exception ex) {
                System.err.println("Advertencia: error ajustando inventario tras cambio de estado: " + ex.getMessage());
            }
        }
        return ok;
    }

    // =============================
    // Lógica de ajuste de inventario
    // =============================
    private void ajustarStockPorPrestamoCreado(PrestamoZapato p) throws SQLException {
        if (p.getIdVariante() == null || p.getIdVariante() <= 0) return;

        String pie = p.getPie() != null ? p.getPie().trim().toUpperCase() : "";
        boolean debeDescontar = false;
        if (pie.equals("AMBOS")) {
            debeDescontar = true; // siempre consume un par completo
        } else if (pie.equals("DERECHO") || pie.equals("IZQUIERDO")) {
            // Solo descontar si el usuario eligió "Disponible - Nuevo"
            debeDescontar = p.isNuevoPar();
        }

        if (debeDescontar) {
            ServiceProductVariant spv = new ServiceProductVariant();
            boolean ok = spv.reducePairsStock(p.getIdVariante(), 1);
            if (!ok) {
                throw new SQLException("No hay stock de pares suficiente para reservar por préstamo");
            }
        }
    }

    private void ajustarStockPorPrestamoDevuelto(PrestamoZapato p) throws SQLException {
        if (p.getIdVariante() == null || p.getIdVariante() <= 0) return;

        String pieDevuelto = p.getPie() != null ? p.getPie().trim().toUpperCase() : "";
        // Nueva regla: reponer stock solo cuando ambos pies estén DEVUELTOS
        ServiceProductVariant spv = new ServiceProductVariant();
        if (pieDevuelto.equals("AMBOS")) {
            // Si se devuelven ambos directamente, reponer 1 par
            spv.addPairsStock(p.getIdVariante(), 1);
            return;
        }

        if (pieDevuelto.equals("DERECHO") || pieDevuelto.equals("IZQUIERDO")) {
            // Reponer únicamente si el complemento ya está marcado como DEVUELTO
            boolean complementoDevuelto = dao.existeComplementoDevueltoParaVariante(p.getIdVariante(), pieDevuelto);
            if (complementoDevuelto) {
                spv.addPairsStock(p.getIdVariante(), 1);
            }
        }
    }

    // Utilidad para tablas: mapea prestamos a filas tipo mapa
    public List<Map<String, Object>> listarPrestamosParaTabla(String estado) throws SQLException {
        List<PrestamoZapato> prestamos = listarPrestamos(estado, null, null, null);
        List<Map<String, Object>> resultado = new ArrayList<>();
        ServiceProductVariant spv = new ServiceProductVariant();
        for (PrestamoZapato p : prestamos) {
            Map<String, Object> fila = new HashMap<>();
            fila.put("id", p.getIdPrestamo());
            fila.put("fecha", p.getFechaPrestamo());
            fila.put("estado", p.getEstado());
            fila.put("cliente", p.getNombrePrestatario());
            fila.put("bodega", p.getNombreBodega());
            fila.put("producto", p.getNombreProducto());
            fila.put("talla", p.getTalla());
            fila.put("color", p.getColor());
            // Dirección del prestatario para mostrar en la tabla
            fila.put("direccion", p.getDireccionPrestatario());
            fila.put("pie", p.getPie());
            fila.put("observaciones", p.getObservaciones());
            // Datos adicionales para UI (imagen por id_variante)
            Integer idVar = p.getIdVariante();
            fila.put("idVariante", idVar);
            byte[] imagen = null;
            if (idVar != null && idVar > 0) {
                imagen = spv.getVariantImage(idVar);
            }
            fila.put("imagenVariante", imagen);
            resultado.add(fila);
        }
        return resultado;
    }
}
