package raven.clases.comercial;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JOptionPane;
import raven.controlador.principal.conexion;
import raven.controlador.comercial.ModelCliente;
import raven.clases.admin.UserSession;

public class ServiceCliente {

    public List<ModelCliente> getAll() throws SQLException {
        String sql = "SELECT "
                + "id_cliente, "
                + "nombre, "
                + "dni, "
                + "direccion, "
                + "telefono, "
                + "email, "
                + "fecha_registro, "
                + "activo, "
                + "id_bodega "
                + "FROM clientes "
                + "WHERE id_bodega = ? "
                + "ORDER BY nombre";

        try (Connection con = conexion.getInstance().createConnection(); PreparedStatement p = con.prepareStatement(sql)) {
            p.setInt(1, UserSession.getInstance().getCurrentUser().getIdBodega());
            try (ResultSet r = p.executeQuery()) {
                List<ModelCliente> clientes = new ArrayList<>();
                while (r.next()) {
                    ModelCliente cliente = new ModelCliente();
                    cliente.setIdCliente(r.getInt("id_cliente"));
                    cliente.setNombre(r.getString("nombre"));
                    cliente.setDni(r.getString("dni"));
                    cliente.setDireccion(r.getString("direccion"));
                    cliente.setTelefono(r.getString("telefono"));
                    cliente.setEmail(r.getString("email"));
                    cliente.setFechaRegistro(r.getTimestamp("fecha_registro"));
                    cliente.setActivo(r.getBoolean("activo"));
                    cliente.setIdBodega(r.getInt("id_bodega"));

                    clientes.add(cliente);
                }
                return clientes;
            }
        }
    }

    public int getIdByNombre(String nombre) throws SQLException {
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            con = conexion.getInstance().createConnection();
            String sql = "SELECT id_cliente FROM clientes WHERE nombre = ? AND id_bodega = ?";
            ps = con.prepareStatement(sql);
            ps.setString(1, nombre);
            ps.setInt(2, UserSession.getInstance().getCurrentUser().getIdBodega());
            rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getInt("id_cliente");
            }
            return -1;
        } finally {
            conexion.getInstance().close(rs, ps, con);
        }
    }

    public void create(ModelCliente cliente) throws SQLException {
        String sql = "INSERT INTO clientes (nombre, dni, direccion, telefono, email, id_bodega) "
                + "VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection con = conexion.getInstance().createConnection(); PreparedStatement p = con.prepareStatement(sql)) {

            p.setString(1, cliente.getNombre());
            p.setString(2, cliente.getDni());
            p.setString(3, cliente.getDireccion());
            p.setString(4, cliente.getTelefono());
            p.setString(5, cliente.getEmail());
            p.setInt(6, cliente.getIdBodega());

            int rowsAffected = p.executeUpdate();
            if (rowsAffected == 0) {
                JOptionPane.showMessageDialog(null, "No se pudo crear el cliente");
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error en la base de datos: " + e.getMessage());
            throw e;
        }
    }

    public List<ModelCliente> search(String searchTerm) throws SQLException {
        String sql = "SELECT * FROM clientes "
                + "WHERE activo = 1 AND id_bodega = ? AND ("
                + "    LOWER(nombre) LIKE LOWER(?) OR "
                + "    dni LIKE ? OR "
                + "    LOWER(email) LIKE LOWER(?) OR "
                + "    telefono LIKE ? OR "
                + "    CAST(id_cliente AS CHAR) LIKE ?) "
                + "ORDER BY nombre";

        try (Connection con = conexion.getInstance().createConnection(); PreparedStatement p = con.prepareStatement(sql)) {

            String searchPattern = "%" + searchTerm.toLowerCase() + "%";
            String numericSearch = "%" + searchTerm + "%";

            p.setInt(1, UserSession.getInstance().getCurrentUser().getIdBodega());
            p.setString(2, searchPattern);
            p.setString(3, numericSearch);
            p.setString(4, searchPattern);
            p.setString(5, numericSearch);
            p.setString(6, numericSearch);

            try (ResultSet r = p.executeQuery()) {
                List<ModelCliente> clientes = new ArrayList<>();
                while (r.next()) {
                    clientes.add(mapResultSetToCliente(r));
                }
                return clientes;
            }
        }
    }

    public void update(ModelCliente cliente) throws SQLException {
        String sql = "UPDATE clientes SET "
                + "nombre = ?, "
                + "dni = ?, "
                + "direccion = ?, "
                + "telefono = ?, "
                + "email = ?, "
                + "puntos_acumulados = ? "
                + "WHERE id_cliente = ?";

        try (Connection con = conexion.getInstance().createConnection(); PreparedStatement p = con.prepareStatement(sql)) {

            p.setString(1, cliente.getNombre());
            p.setString(2, cliente.getDni());
            p.setString(3, cliente.getDireccion());
            p.setString(4, cliente.getTelefono());
            p.setString(5, cliente.getEmail());
            p.setInt(6, cliente.getPuntosAcumulados());
            p.setInt(7, cliente.getIdCliente());

            int rowsAffected = p.executeUpdate();
            if (rowsAffected == 0) {
                JOptionPane.showMessageDialog(null, "No se encontró el cliente a actualizar");
            }
        } catch (SQLException e) {
            String errorMsg = "Error al actualizar cliente: ";
            if (e.getSQLState().equals("23505")) { // Violación de constraint única
                errorMsg += "El DNI ya existe para otro cliente.";
            } else {
                errorMsg += e.getMessage();
            }
            JOptionPane.showMessageDialog(null, errorMsg, "Error", JOptionPane.ERROR_MESSAGE);
            throw e;
        }
    }

    public void delete(int idCliente) throws SQLException {
        String sql = "UPDATE clientes SET activo = 0 WHERE id_cliente = ?";

        try (Connection con = conexion.getInstance().createConnection(); PreparedStatement p = con.prepareStatement(sql)) {

            p.setInt(1, idCliente);
            p.executeUpdate();
        }
    }

    public void active(int idCliente) throws SQLException {
        String sql = "UPDATE clientes SET activo = 1 WHERE id_cliente = ?";

        try (Connection con = conexion.getInstance().createConnection(); PreparedStatement p = con.prepareStatement(sql)) {

            p.setInt(1, idCliente);
            p.executeUpdate();
        }
    }

    private ModelCliente mapResultSetToCliente(ResultSet r) throws SQLException {
        ModelCliente cliente = new ModelCliente();
        cliente.setIdCliente(r.getInt("id_cliente"));
        cliente.setNombre(r.getString("nombre"));
        cliente.setDni(r.getString("dni"));
        cliente.setDireccion(r.getString("direccion"));
        cliente.setTelefono(r.getString("telefono"));
        cliente.setEmail(r.getString("email"));
        cliente.setFechaRegistro(r.getTimestamp("fecha_registro"));
        cliente.setPuntosAcumulados(r.getInt("puntos_acumulados"));
        cliente.setActivo(r.getBoolean("activo"));
        cliente.setIdBodega(r.getInt("id_bodega"));
        return cliente;
    }

    public ModelCliente obtenerCliente(int idCliente, Connection con) throws SQLException {
        String sql = "SELECT * FROM clientes WHERE id_cliente = ?";

        try (PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, idCliente);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    ModelCliente cliente = new ModelCliente();
                    cliente.setIdCliente(rs.getInt("id_cliente"));
                    cliente.setNombre(rs.getString("nombre"));
                    cliente.setDni(rs.getString("dni"));
                    cliente.setDireccion(rs.getString("direccion"));
                    cliente.setTelefono(rs.getString("telefono"));
                    cliente.setEmail(rs.getString("email"));
                    cliente.setFechaRegistro(rs.getTimestamp("fecha_registro"));
                    cliente.setPuntosAcumulados(rs.getInt("puntos_acumulados"));
                    cliente.setActivo(rs.getBoolean("activo"));
                    cliente.setIdBodega(rs.getInt("id_bodega"));

                    return cliente;
                }
            }
        }

        return null; // O lanzar una excepción si prefieres
    }

}
