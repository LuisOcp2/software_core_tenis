package raven.clases.admin;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JOptionPane;
import raven.controlador.admin.ModelCaja;
import raven.controlador.principal.conexion;

public class ServiceCaja {

    private conexion conexionDB = conexion.getInstance();

    // Obtener todas las cajas activas
    public List<ModelCaja> getAll() throws SQLException {
        String sql = "SELECT id_caja, nombre, ubicacion, activa FROM cajas WHERE activa = 1";
        List<ModelCaja> cajas = new ArrayList<>();

        try (Connection con = conexionDB.createConnection(); PreparedStatement p = con.prepareStatement(sql); ResultSet r = p.executeQuery()) {

            while (r.next()) {
                ModelCaja caja = new ModelCaja();
                caja.setIdCaja(r.getInt("id_caja"));
                caja.setNombre(r.getString("nombre"));
                caja.setUbicacion(r.getString("ubicacion"));
                caja.setActiva(r.getBoolean("activa"));
                cajas.add(caja);
            }
        }
        return cajas;
    }

    // Crear una nueva caja
    public void create(ModelCaja caja) throws SQLException {
        String sql = "INSERT INTO cajas (nombre, ubicacion, activa) VALUES (?, ?, 1)";

        try (Connection con = conexionDB.createConnection(); PreparedStatement p = con.prepareStatement(sql)) {

            p.setString(1, caja.getNombre());
            p.setString(2, caja.getUbicacion());
            p.executeUpdate();

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error al crear caja: " + e.getMessage());
            throw e;
        }
    }

    // Actualizar una caja existente
    public void update(ModelCaja caja) throws SQLException {
        String sql = "UPDATE cajas SET nombre = ?, ubicacion = ? WHERE id_caja = ?";

        try (Connection con = conexionDB.createConnection(); PreparedStatement p = con.prepareStatement(sql)) {

            p.setString(1, caja.getNombre());
            p.setString(2, caja.getUbicacion());
            p.setInt(3, caja.getIdCaja());

            int rowsAffected = p.executeUpdate();
            if (rowsAffected == 0) {
                JOptionPane.showMessageDialog(null, "Caja no encontrada");
            }

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error al actualizar: " + e.getMessage());
            throw e;
        }
    }

    // Desactivar una caja (no se elimina físicamente)
    public void delete(int idCaja) throws SQLException {
        String sql = "UPDATE cajas SET activa = 0 WHERE id_caja = ?";

        try (Connection con = conexionDB.createConnection(); PreparedStatement p = con.prepareStatement(sql)) {

            p.setInt(1, idCaja);
            p.executeUpdate();

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error al eliminar caja: " + e.getMessage());
            throw e;
        }
    }

    // Buscar cajas por nombre o ubicación
    public List<ModelCaja> search(String searchTerm) throws SQLException {
        String sql = "SELECT id_caja, nombre, ubicacion, activa FROM cajas "
                + "WHERE activa = 1 AND (LOWER(nombre) LIKE LOWER(?) OR LOWER(ubicacion) LIKE LOWER(?))";
        List<ModelCaja> cajas = new ArrayList<>();
        String searchPattern = "%" + searchTerm.toLowerCase() + "%";

        try (Connection con = conexionDB.createConnection(); PreparedStatement p = con.prepareStatement(sql)) {

            p.setString(1, searchPattern);
            p.setString(2, searchPattern);

            try (ResultSet r = p.executeQuery()) {
                while (r.next()) {
                    ModelCaja caja = new ModelCaja();
                    caja.setIdCaja(r.getInt("id_caja"));
                    caja.setNombre(r.getString("nombre"));
                    caja.setUbicacion(r.getString("ubicacion"));
                    caja.setActiva(r.getBoolean("activa"));
                    cajas.add(caja);
                }
            }
        }
        return cajas;
    }

    /**
     * Obtiene la caja asociada a una bodega.
     *
     * @param idBodega ID de la bodega
     * @return ModelCaja de la bodega, o null si no existe
     * @throws SQLException si hay error en BD
     */
    public ModelCaja obtenerCajaPorBodega(int idBodega) throws SQLException {
        String sql = "SELECT * FROM cajas WHERE id_bodega = ? AND activa = 1 LIMIT 1";

        try (Connection conn = conexion.getInstance().createConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, idBodega);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapearCaja(rs);
                }
            }
        }

        return null;
    }

    /**
     * Obtiene una caja por su ID.
     */
    public ModelCaja obtenerCajaPorId(int idCaja) throws SQLException {
        String sql = "SELECT * FROM cajas WHERE id_caja = ?";

        try (Connection conn = conexion.getInstance().createConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, idCaja);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapearCaja(rs);
                }
            }
        }

        return null;
    }

    /**
     * Lista todas las cajas activas.
     */
    public List<ModelCaja> obtenerCajasActivas() throws SQLException {
        List<ModelCaja> cajas = new ArrayList<>();
        String sql = "SELECT * FROM cajas WHERE activa = 1";

        try (Connection conn = conexion.getInstance().createConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                cajas.add(mapearCaja(rs));
            }
        }

        return cajas;
    }

    /**
     * Mapea un ResultSet a ModelCaja.
     */
    private ModelCaja mapearCaja(ResultSet rs) throws SQLException {
        ModelCaja caja = new ModelCaja();
        caja.setIdCaja(rs.getInt("id_caja"));
        caja.setNombre(rs.getString("nombre"));
        caja.setUbicacion(rs.getString("ubicacion"));
        caja.setActiva(rs.getBoolean("activa"));

        return caja;
    }
}
