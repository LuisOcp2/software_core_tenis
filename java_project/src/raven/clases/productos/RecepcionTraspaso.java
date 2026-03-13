package raven.clases.productos;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Frame;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import raven.application.form.productos.traspasos.RecepcionTraspasoForm;
import raven.controlador.principal.conexion;
import raven.utils.NotificacionesService;

/**
 * Helper para manejar la recepción de traspasos.
 * Invoca el nuevo formulario RecepcionTraspasoForm y procesa la transacción.
 */
public class RecepcionTraspaso {

    public interface RecepcionCallback {
        void onRecepcionExitosa(String numeroTraspaso, List<Map<String, Object>> productosRecibidos);

        void onRecepcionCancelada();
    }

    public static void recibirTraspaso(java.awt.Window parent, String numeroTraspaso, RecepcionCallback callback) {
        SwingUtilities.invokeLater(() -> {
            try {
                // 1. Verificar estado
                if (!verificarEstadoTraspaso(numeroTraspaso, "en_transito")) {
                    JOptionPane.showMessageDialog(parent,
                            "El traspaso debe estar EN TRÁNSITO para poder recibirlo.",
                            "Estado Incorrecto", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                // 2. Fetch data
                Map<String, String> headerInfo = obtenerCabeceraTraspaso(numeroTraspaso);
                List<Map<String, Object>> productos = obtenerProductosEnviados(numeroTraspaso);

                // 3. Setup Dialog
                JDialog dialog = new JDialog(parent, "Recepción de Traspaso " + numeroTraspaso,
                        java.awt.Dialog.ModalityType.APPLICATION_MODAL);
                dialog.setSize(1100, 800);
                dialog.setLocationRelativeTo(parent);
                dialog.setLayout(new BorderLayout());

                RecepcionTraspasoForm form = new RecepcionTraspasoForm();

                // Populate Form
                form.setTraspasoInfo(
                        numeroTraspaso,
                        headerInfo.get("bodega_origen"),
                        headerInfo.get("bodega_destino"),
                        headerInfo.get("fecha_envio"));
                form.setProductos(productos);

                // 4. Events
                form.btnCancelar.addActionListener(e -> {
                    dialog.dispose();
                    if (callback != null)
                        callback.onRecepcionCancelada();
                });

                form.btnConfirmar.addActionListener(e -> {
                    int confirm = JOptionPane.showConfirmDialog(dialog,
                            "¿Está seguro de confirmar la recepción?\nEsto actualizará el inventario.",
                            "Confirmar", JOptionPane.YES_NO_OPTION);

                    if (confirm == JOptionPane.YES_OPTION) {
                        try {
                            List<Map<String, Object>> recibidos = form.getProductosConfirmados();
                            String observaciones = form.getObservaciones();

                            procesarRecepcionBD(numeroTraspaso, recibidos, observaciones);

                            dialog.dispose();
                            if (callback != null)
                                callback.onRecepcionExitosa(numeroTraspaso, recibidos);

                        } catch (Exception ex) {
                            ex.printStackTrace();
                            JOptionPane.showMessageDialog(dialog, "Error al procesar: " + ex.getMessage(), "Error",
                                    JOptionPane.ERROR_MESSAGE);
                        }
                    }
                });

                dialog.add(form, BorderLayout.CENTER);
                dialog.setVisible(true);

            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(parent, "Error iniciando recepción: " + e.getMessage());
            }
        });
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // LOGICA BD
    // ═══════════════════════════════════════════════════════════════════════════

    private static void procesarRecepcionBD(String numeroTraspaso, List<Map<String, Object>> items, String obsGeneral)
            throws Exception {
        Connection conn = conexion.getInstance().createConnection();
        conn.setAutoCommit(false);
        try {
            // 1. Update Traspaso Header
            // 1. Update Traspaso Header
            int idUsuario = raven.controlador.admin.SessionManager.getInstance().getCurrentUserId();

            // Validation and Fallback
            if (idUsuario <= 0) {
                try {
                    idUsuario = raven.clases.admin.UserSession.getInstance().getCurrentUser().getIdUsuario();
                } catch (Exception e) {
                    // ignore
                }
            }

            if (idUsuario <= 0) {
                // Last resort: check if there is a logged user in the main window context or
                // throw error
                throw new Exception(
                        "No se pudo identificar al usuario que recibe el traspaso. Inicie sesión nuevamente.");
            }

            String sqlHead = "UPDATE traspasos SET estado='recibido', fecha_recepcion=NOW(), id_usuario_recibe=?, observaciones=CONCAT(COALESCE(observaciones,''), ?), monto_recibido=? WHERE numero_traspaso=?";

            // Calculate total received
            java.math.BigDecimal montoRecibido = java.math.BigDecimal.ZERO;
            for (Map<String, Object> item : items) {
                int cantidad = getInt(item.get("cantidad_recibida_real"));
                java.math.BigDecimal precio = (java.math.BigDecimal) item.get("precio_unitario");
                if (precio == null)
                    precio = java.math.BigDecimal.ZERO;
                if (cantidad > 0) {
                    montoRecibido = montoRecibido.add(precio.multiply(java.math.BigDecimal.valueOf(cantidad)));
                }
            }

            try (PreparedStatement ps = conn.prepareStatement(sqlHead)) {
                ps.setInt(1, idUsuario);
                ps.setString(2, obsGeneral.isEmpty() ? "" : " - RX: " + obsGeneral);
                ps.setBigDecimal(3, montoRecibido);
                ps.setString(4, numeroTraspaso);
                ps.executeUpdate();
            }

            // 2. Resolve Bodega Destino
            int idBodegaDestino = 0;
            try (PreparedStatement ps = conn
                    .prepareStatement("SELECT id_bodega_destino FROM traspasos WHERE numero_traspaso=?")) {
                ps.setString(1, numeroTraspaso);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next())
                        idBodegaDestino = rs.getInt(1);
                }
            }
            if (idBodegaDestino == 0)
                throw new Exception("No se encontró bodega destino");

            // 3. Update Details and Inventory
            String sqlDet = "UPDATE traspaso_detalles SET cantidad_recibida=?, estado_detalle='recibido' WHERE id_detalle_traspaso=?";
            String sqlInvUpdatePar = "UPDATE inventario_bodega SET Stock_par = COALESCE(Stock_par,0) + ?, activo=1, fecha_ultimo_movimiento=NOW() WHERE id_bodega=? AND id_variante=?";
            String sqlInvUpdateCaja = "UPDATE inventario_bodega SET Stock_caja = COALESCE(Stock_caja,0) + ?, activo=1, fecha_ultimo_movimiento=NOW() WHERE id_bodega=? AND id_variante=?";
            String sqlInvInsert = "INSERT INTO inventario_bodega (id_bodega, id_variante, Stock_par, Stock_caja, activo, fecha_ultimo_movimiento, stock_reservado) VALUES (?,?,?,?,1,NOW(),0)";
            String sqlCheckInv = "SELECT 1 FROM inventario_bodega WHERE id_bodega=? AND id_variante=?";

            try (PreparedStatement psDet = conn.prepareStatement(sqlDet);
                    PreparedStatement psUpPar = conn.prepareStatement(sqlInvUpdatePar);
                    PreparedStatement psUpCaja = conn.prepareStatement(sqlInvUpdateCaja);
                    PreparedStatement psIns = conn.prepareStatement(sqlInvInsert);
                    PreparedStatement psCheck = conn.prepareStatement(sqlCheckInv)) {

                for (Map<String, Object> item : items) {
                    int idDetalle = getInt(item.get("id_detalle")); // From original map
                    int cantidad = getInt(item.get("cantidad_recibida_real"));
                    int idVariante = getInt(item.get("id_variante"));
                    String tipo = getString(item.get("tipo_detalle")); // par or caja

                    // Update Detail
                    psDet.setInt(1, cantidad);
                    psDet.setInt(2, idDetalle);
                    psDet.addBatch();

                    // Update Inventory
                    boolean isCaja = "caja".equalsIgnoreCase(tipo);
                    int qtyPar = isCaja ? 0 : cantidad;
                    int qtyCaja = isCaja ? cantidad : 0; // Assuming 1-to-1 logic for simplicity if it is caja type

                    // If type is caja, we might need to know how many pares inside, but typically
                    // stock_caja stores # of boxes.
                    // Implementation assumes 'cantidad' is in the unit specified by 'tipo'.

                    psCheck.setInt(1, idBodegaDestino);
                    psCheck.setInt(2, idVariante);
                    boolean exists = false;
                    try (ResultSet rs = psCheck.executeQuery()) {
                        exists = rs.next();
                    }

                    if (exists) {
                        if (isCaja) {
                            psUpCaja.setInt(1, qtyCaja);
                            psUpCaja.setInt(2, idBodegaDestino);
                            psUpCaja.setInt(3, idVariante);
                            psUpCaja.addBatch();
                        } else {
                            psUpPar.setInt(1, qtyPar);
                            psUpPar.setInt(2, idBodegaDestino);
                            psUpPar.setInt(3, idVariante);
                            psUpPar.addBatch();
                        }
                    } else {
                        psIns.setInt(1, idBodegaDestino);
                        psIns.setInt(2, idVariante);
                        psIns.setInt(3, qtyPar);
                        psIns.setInt(4, qtyCaja);
                        psIns.addBatch();
                    }
                }
                psDet.executeBatch();
                psUpPar.executeBatch();
                psUpCaja.executeBatch();
                psIns.executeBatch();
            }

            conn.commit();

            // 4. Notifications (Cleanup)
            try {
                // Determine ID Traspaso
                int idTraspaso = 0;
                try (PreparedStatement psH = conn
                        .prepareStatement("SELECT id_traspaso FROM traspasos WHERE numero_traspaso=?")) {
                    psH.setString(1, numeroTraspaso);
                    try (ResultSet rs = psH.executeQuery()) {
                        if (rs.next())
                            idTraspaso = rs.getInt(1);
                    }
                }
                if (idTraspaso > 0) {
                    NotificacionesService.getInstance().marcarNotificacionesTraspasoComoLeidas(idTraspaso);
                }
            } catch (Exception e) {
                System.err.println("Warning: Notif cleanup failed: " + e.getMessage());
            }

        } catch (Exception e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
            conn.close();
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // HELPERS DB
    // ═══════════════════════════════════════════════════════════════════════════

    private static boolean verificarEstadoTraspaso(String numero, String estadoReq) throws SQLException {
        try (Connection con = conexion.getInstance().createConnection();
                PreparedStatement ps = con.prepareStatement("SELECT estado FROM traspasos WHERE numero_traspaso=?")) {
            ps.setString(1, numero);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next())
                    return estadoReq.equalsIgnoreCase(rs.getString(1));
            }
        }
        return false;
    }

    private static Map<String, String> obtenerCabeceraTraspaso(String numero) throws SQLException {
        Map<String, String> map = new HashMap<>();
        String sql = "SELECT bo.nombre as ori, bd.nombre as des, t.fecha_envio " +
                "FROM traspasos t " +
                "LEFT JOIN bodegas bo ON t.id_bodega_origen = bo.id_bodega " +
                "LEFT JOIN bodegas bd ON t.id_bodega_destino = bd.id_bodega " +
                "WHERE t.numero_traspaso = ?";
        try (Connection con = conexion.getInstance().createConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, numero);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    map.put("bodega_origen", rs.getString("ori"));
                    map.put("bodega_destino", rs.getString("des"));
                    map.put("fecha_envio", rs.getString("fecha_envio"));
                }
            }
        }
        return map;
    }

    private static List<Map<String, Object>> obtenerProductosEnviados(String numero) throws SQLException {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = "SELECT td.id_detalle_traspaso as id_detalle, td.id_producto, td.id_variante, td.cantidad_enviada, "
                + "td.tipo as tipo_detalle, p.nombre as producto_nombre, pv.sku, "
                + "c.nombre as color_nombre, t.numero as talla_numero, "
                + "td.precio_unitario as precio_unitario " // Added
                + "FROM traspaso_detalles td "
                + "JOIN traspasos tr ON td.id_traspaso = tr.id_traspaso "
                + "JOIN productos p ON td.id_producto = p.id_producto "
                + "LEFT JOIN producto_variantes pv ON td.id_variante = pv.id_variante "
                + "LEFT JOIN colores c ON pv.id_color = c.id_color "
                + "LEFT JOIN tallas t ON pv.id_talla = t.id_talla "
                + "WHERE tr.numero_traspaso = ? AND td.cantidad_enviada > 0";

        try (Connection con = conexion.getInstance().createConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, numero);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id_detalle", rs.getInt("id_detalle"));
                    m.put("id_producto", rs.getInt("id_producto"));
                    m.put("id_variante", rs.getInt("id_variante"));
                    m.put("cantidad_enviada", rs.getInt("cantidad_enviada"));
                    m.put("tipo_detalle", rs.getString("tipo_detalle"));
                    m.put("producto_nombre", rs.getString("producto_nombre"));
                    m.put("sku", rs.getString("sku"));
                    m.put("color_nombre", rs.getString("color_nombre"));
                    m.put("talla_numero", rs.getString("talla_numero"));
                    m.put("precio_unitario", rs.getBigDecimal("precio_unitario")); // Added
                    list.add(m);
                }
            }
        }
        return list;
    }

    private static int getInt(Object o) {
        if (o instanceof Number)
            return ((Number) o).intValue();
        return 0;
    }

    private static String getString(Object o) {
        return o == null ? "" : o.toString();
    }
}