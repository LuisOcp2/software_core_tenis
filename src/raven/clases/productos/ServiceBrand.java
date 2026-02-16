package raven.clases.productos;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JOptionPane;
import raven.controlador.principal.conexion;
import raven.controlador.productos.ModelBrand;

public class ServiceBrand {

    public List<ModelBrand> getAll() throws SQLException {
        String sql = "SELECT id_marca, nombre, descripcion, activo "
                + "FROM marcas WHERE activo = 1 ORDER BY nombre";

        List<ModelBrand> brands = new ArrayList<>();

        try (Connection con = conexion.getInstance().createConnection(); PreparedStatement p = con.prepareStatement(sql); ResultSet r = p.executeQuery()) {

            while (r.next()) {
                ModelBrand brand = new ModelBrand();
                brand.setBrandId(r.getInt("id_marca"));
                brand.setName(r.getString("nombre"));
                brand.setDescription(r.getString("descripcion"));
                brand.setActive(r.getBoolean("activo"));
                brands.add(brand);
            }
        }
        return brands;
    }

    public void create(ModelBrand brand) throws SQLException {
        String sql = "INSERT INTO marcas (nombre, descripcion, activo) VALUES (?, ?, 1)";

        try (Connection con = conexion.getInstance().createConnection(); PreparedStatement p = con.prepareStatement(sql)) {

            p.setString(1, brand.getName());
            p.setString(2, brand.getDescription());
            p.executeUpdate();

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error al crear marca: " + e.getMessage());
            throw e;
        }
    }

    public void update(ModelBrand brand) throws SQLException {
        String sql = "UPDATE marcas SET nombre = ?, descripcion = ? WHERE id_marca = ?";

        try (Connection con = conexion.getInstance().createConnection(); PreparedStatement p = con.prepareStatement(sql)) {

            p.setString(1, brand.getName());
            p.setString(2, brand.getDescription());
            p.setInt(3, brand.getBrandId());

            int rowsAffected = p.executeUpdate();
            if (rowsAffected == 0) {
                JOptionPane.showMessageDialog(null, "Marca no encontrada");
            }

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error al actualizar: " + e.getMessage());
            throw e;
        }
    }

    public void delete(int brandId) throws SQLException {
        String sql = "UPDATE marcas SET activo = 0 WHERE id_marca = ?";

        try (Connection con = conexion.getInstance().createConnection(); PreparedStatement p = con.prepareStatement(sql)) {

            p.setInt(1, brandId);
            p.executeUpdate();

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error al eliminar marca: " + e.getMessage());
            throw e;
        }
    }

    public List<ModelBrand> search(String searchTerm) throws SQLException {
        String sql = "SELECT id_marca, nombre, descripcion, activo "
                + "FROM marcas WHERE activo = 1 "
                + "AND (LOWER(nombre) LIKE LOWER(?) OR LOWER(descripcion) LIKE LOWER(?)) "
                + "ORDER BY nombre";

        List<ModelBrand> brands = new ArrayList<>();
        String searchPattern = "%" + searchTerm.toLowerCase() + "%";

        try (Connection con = conexion.getInstance().createConnection(); PreparedStatement p = con.prepareStatement(sql)) {

            p.setString(1, searchPattern);
            p.setString(2, searchPattern);

            try (ResultSet r = p.executeQuery()) {
                while (r.next()) {
                    ModelBrand brand = new ModelBrand();
                    brand.setBrandId(r.getInt("id_marca"));
                    brand.setName(r.getString("nombre"));
                    brand.setDescription(r.getString("descripcion"));
                    brand.setActive(r.getBoolean("activo"));
                    brands.add(brand);
                }
            }
        }
        return brands;
    }

    public boolean existeMarca(String nombre) {
        String sql = "SELECT 1 FROM marcas WHERE nombre = ? AND activo = 1 LIMIT 1";
        try (Connection con = conexion.getInstance().createConnection();
             PreparedStatement p = con.prepareStatement(sql)) {
            p.setString(1, nombre);
            try (ResultSet r = p.executeQuery()) {
                return r.next();
            }
        } catch (SQLException e) {
            System.err.println("Error al verificar existencia de marca: " + e.getMessage());
            return false;
        }
    }
}
