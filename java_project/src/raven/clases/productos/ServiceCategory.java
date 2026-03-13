package raven.clases.productos;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JOptionPane;
import raven.controlador.principal.conexion;
import raven.controlador.productos.ModelCategory;

public class ServiceCategory {

    public List<ModelCategory> getAll() throws SQLException {
        String sql = "SELECT id_categoria, nombre, descripcion, activo "
                   + "FROM categorias WHERE activo = 1 ORDER BY nombre";
        
        List<ModelCategory> categories = new ArrayList<>();
        
        try (Connection con = conexion.getInstance().createConnection();
             PreparedStatement p = con.prepareStatement(sql);
             ResultSet r = p.executeQuery()) {
            
            while (r.next()) {
                ModelCategory category = new ModelCategory();
                category.setCategoryId(r.getInt("id_categoria"));
                category.setName(r.getString("nombre"));
                category.setDescription(r.getString("descripcion"));
                category.setActive(r.getBoolean("activo"));
                categories.add(category);
            }
        }
        return categories;
    }

    public void create(ModelCategory category) throws SQLException {
        String sql = "INSERT INTO categorias (nombre, descripcion, activo) VALUES (?, ?, 1)";
        
        try (Connection con = conexion.getInstance().createConnection();
             PreparedStatement p = con.prepareStatement(sql)) {
            
            p.setString(1, category.getName());
            p.setString(2, category.getDescription());
            p.executeUpdate();
            
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error al crear categoría: " + e.getMessage());
            throw e;
        }
    }

    public void update(ModelCategory category) throws SQLException {
        String sql = "UPDATE categorias SET nombre = ?, descripcion = ? WHERE id_categoria = ?";
        
        try (Connection con = conexion.getInstance().createConnection();
             PreparedStatement p = con.prepareStatement(sql)) {
            
            p.setString(1, category.getName());
            p.setString(2, category.getDescription());
            p.setInt(3, category.getCategoryId());
            
            int rowsAffected = p.executeUpdate();
            if (rowsAffected == 0) {
                JOptionPane.showMessageDialog(null, "Categoría no encontrada");
            }
            
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error al actualizar: " + e.getMessage());
            throw e;
        }
    }

    public void delete(int categoryId) throws SQLException {
        String sql = "UPDATE categorias SET activo = 0 WHERE id_categoria = ?";
        
        try (Connection con = conexion.getInstance().createConnection();
             PreparedStatement p = con.prepareStatement(sql)) {
            
            p.setInt(1, categoryId);
            p.executeUpdate();
            
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error al eliminar categoría: " + e.getMessage());
            throw e;
        }
    }

    public List<ModelCategory> search(String searchTerm) throws SQLException {
        String sql = "SELECT id_categoria, nombre, descripcion, activo "
                   + "FROM categorias WHERE activo = 1 "
                   + "AND (LOWER(nombre) LIKE LOWER(?) OR LOWER(descripcion) LIKE LOWER(?)) "
                   + "ORDER BY nombre";
        
        List<ModelCategory> categories = new ArrayList<>();
        String searchPattern = "%" + searchTerm.toLowerCase() + "%";
        
        try (Connection con = conexion.getInstance().createConnection();
             PreparedStatement p = con.prepareStatement(sql)) {
            
            p.setString(1, searchPattern);
            p.setString(2, searchPattern);
            
            try (ResultSet r = p.executeQuery()) {
                while (r.next()) {
                    ModelCategory category = new ModelCategory();
                    category.setCategoryId(r.getInt("id_categoria"));
                    category.setName(r.getString("nombre"));
                    category.setDescription(r.getString("descripcion"));
                    category.setActive(r.getBoolean("activo"));
                    categories.add(category);
                }
            }
        }
        return categories;
    }

    public boolean existeCategoria(String nombre) {
        String sql = "SELECT 1 FROM categorias WHERE nombre = ? AND activo = 1 LIMIT 1";
        try (Connection con = conexion.getInstance().createConnection();
             PreparedStatement p = con.prepareStatement(sql)) {
            p.setString(1, nombre);
            try (ResultSet r = p.executeQuery()) {
                return r.next();
            }
        } catch (SQLException e) {
            System.err.println("Error al verificar existencia de categoría: " + e.getMessage());
            return false;
        }
    }
}