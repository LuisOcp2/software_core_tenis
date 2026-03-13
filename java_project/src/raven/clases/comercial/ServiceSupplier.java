package raven.clases.comercial;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JOptionPane;
import raven.controlador.principal.conexion;
import raven.controlador.comercial.ModelSupplier;

public class ServiceSupplier {

    public List<ModelSupplier> getAll() throws SQLException {
        String sql = "SELECT id_proveedor, nombre, ruc, direccion, telefono, email, activo "
                   + "FROM proveedores WHERE activo = 1 ORDER BY nombre";
        
        List<ModelSupplier> suppliers = new ArrayList<>();
        
        try (Connection con = conexion.getInstance().createConnection();
             PreparedStatement p = con.prepareStatement(sql);
             ResultSet r = p.executeQuery()) {
            
            while (r.next()) {
                ModelSupplier supplier = new ModelSupplier();
                supplier.setSupplierId(r.getInt("id_proveedor"));
                supplier.setName(r.getString("nombre"));
                supplier.setRuc(r.getString("ruc"));
                supplier.setAddress(r.getString("direccion"));
                supplier.setPhone(r.getString("telefono"));
                supplier.setEmail(r.getString("email"));
                supplier.setActive(r.getBoolean("activo"));
                suppliers.add(supplier);
            }
        }
        return suppliers;
    }

    public void create(ModelSupplier supplier) throws SQLException {
        String sql = "INSERT INTO proveedores (nombre, ruc, direccion, telefono, email, activo) "
                   + "VALUES (?, ?, ?, ?, ?, 1)";
        
        try (Connection con = conexion.getInstance().createConnection();
             PreparedStatement p = con.prepareStatement(sql)) {
            
            p.setString(1, supplier.getName());
            p.setString(2, supplier.getRuc());
            p.setString(3, supplier.getAddress());
            p.setString(4, supplier.getPhone());
            p.setString(5, supplier.getEmail());
            p.executeUpdate();
            
        } catch (SQLException e) {
            throw e;
        }
    }

    public void update(ModelSupplier supplier) throws SQLException {
        String sql = "UPDATE proveedores SET nombre = ?, ruc = ?, direccion = ?, "
                   + "telefono = ?, email = ? WHERE id_proveedor = ?";
        
        try (Connection con = conexion.getInstance().createConnection();
             PreparedStatement p = con.prepareStatement(sql)) {
            
            p.setString(1, supplier.getName());
            p.setString(2, supplier.getRuc());
            p.setString(3, supplier.getAddress());
            p.setString(4, supplier.getPhone());
            p.setString(5, supplier.getEmail());
            p.setInt(6, supplier.getSupplierId());
            
            int rowsAffected = p.executeUpdate();
            if (rowsAffected == 0) {
                throw new SQLException("Proveedor no encontrado");
            }
            
        } catch (SQLException e) {
            throw e;
        }
    }

    public void delete(int supplierId) throws SQLException {
        String sql = "UPDATE proveedores SET activo = 0 WHERE id_proveedor = ?";
        
        try (Connection con = conexion.getInstance().createConnection();
             PreparedStatement p = con.prepareStatement(sql)) {
            
            p.setInt(1, supplierId);
            p.executeUpdate();
            
        } catch (SQLException e) {
            throw e;
        }
    }

    public List<ModelSupplier> search(String searchTerm) throws SQLException {
        String sql = "SELECT * FROM proveedores WHERE activo = 1 "
                   + "AND (LOWER(nombre) LIKE LOWER(?) "
                   + "OR LOWER(ruc) LIKE LOWER(?) "
                   + "OR LOWER(direccion) LIKE LOWER(?) "
                   + "OR telefono LIKE ? "
                   + "OR LOWER(email) LIKE LOWER(?)) "
                   + "ORDER BY nombre";
        
        List<ModelSupplier> suppliers = new ArrayList<>();
        String searchPattern = "%" + searchTerm.toLowerCase() + "%";
        
        try (Connection con = conexion.getInstance().createConnection();
             PreparedStatement p = con.prepareStatement(sql)) {
            
            p.setString(1, searchPattern);
            p.setString(2, searchPattern);
            p.setString(3, searchPattern);
            p.setString(4, "%" + searchTerm + "%");  // Búsqueda literal para teléfono
            p.setString(5, searchPattern);
            
            try (ResultSet r = p.executeQuery()) {
                while (r.next()) {
                    ModelSupplier supplier = new ModelSupplier();
                    supplier.setSupplierId(r.getInt("id_proveedor"));
                    supplier.setName(r.getString("nombre"));
                    supplier.setRuc(r.getString("ruc"));
                    supplier.setAddress(r.getString("direccion"));
                    supplier.setPhone(r.getString("telefono"));
                    supplier.setEmail(r.getString("email"));
                    supplier.setActive(r.getBoolean("activo"));
                    suppliers.add(supplier);
                }
            }
        }
        return suppliers;
    }
}