package raven.clases.admin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import raven.controlador.admin.ModelBodegas;
import raven.controlador.principal.conexion;

public class ServiceBodegas {

    private ModelBodegas mapearBodega(ResultSet rs) throws SQLException {
        ModelBodegas b = new ModelBodegas();
        b.setIdBodega(rs.getInt("id_bodega"));
        b.setCodigo(rs.getString("codigo"));
        b.setNombre(rs.getString("nombre"));
        b.setDireccion(rs.getString("direccion"));
        b.setTelefono(rs.getString("telefono"));
        b.setResponsable(rs.getString("responsable"));
        b.setTipo(rs.getString("tipo"));
        Object cap = rs.getObject("capacidad_maxima");
        b.setCapacidadMaxima(cap != null ? rs.getInt("capacidad_maxima") : null);
        Object act = rs.getObject("activa");
        b.setActiva(act != null ? rs.getBoolean("activa") : Boolean.TRUE);
        return b;
    }

    public List<ModelBodegas> obtenerTodas() throws SQLException {
        String sql = "SELECT id_bodega, codigo, nombre, direccion, telefono, responsable, tipo, capacidad_maxima, activa FROM bodegas ORDER BY nombre";
        List<ModelBodegas> list = new ArrayList<>();
        try (Connection con = conexion.getInstance().createConnection(); PreparedStatement ps = con.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapearBodega(rs));
            }
        }
        return list;
    }

    public List<ModelBodegas> search(String term) throws SQLException {
        String sql = "SELECT id_bodega, codigo, nombre, direccion, telefono, responsable, tipo, capacidad_maxima, activa "
                   + "FROM bodegas WHERE "
                   + "LOWER(codigo) LIKE LOWER(?) OR LOWER(nombre) LIKE LOWER(?) OR LOWER(responsable) LIKE LOWER(?) OR LOWER(tipo) LIKE LOWER(?) "
                   + "ORDER BY nombre";
        List<ModelBodegas> list = new ArrayList<>();
        String like = "%" + term.toLowerCase() + "%";
        try (Connection con = conexion.getInstance().createConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, like);
            ps.setString(2, like);
            ps.setString(3, like);
            ps.setString(4, like);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapearBodega(rs));
                }
            }
        }
        return list;
    }

    public void insertar(ModelBodegas b) throws SQLException {
        String sqlBodega = "INSERT INTO bodegas (codigo, nombre, direccion, telefono, responsable, tipo, capacidad_maxima, activa) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        String sqlCaja = "INSERT INTO cajas (nombre, ubicacion, activa, id_bodega) VALUES (?, ?, 1, ?)";

        Connection con = null;
        PreparedStatement psBodega = null;
        PreparedStatement psCaja = null;

        try {
            con = conexion.getInstance().createConnection();
            con.setAutoCommit(false); //  Inicia transacción

            // Insertar bodega
            psBodega = con.prepareStatement(sqlBodega, PreparedStatement.RETURN_GENERATED_KEYS);
            psBodega.setString(1, b.getCodigo());
            psBodega.setString(2, b.getNombre());
            psBodega.setString(3, b.getDireccion());
            psBodega.setString(4, b.getTelefono());
            psBodega.setString(5, b.getResponsable());
            psBodega.setString(6, b.getTipo());

            if (b.getCapacidadMaxima() != null) {
                psBodega.setInt(7, b.getCapacidadMaxima());
            } else {
                psBodega.setNull(7, java.sql.Types.INTEGER);
            }

            psBodega.setBoolean(8, b.getActiva() == null ? true : b.getActiva());
            psBodega.executeUpdate();

            // Obtener ID generado
            int idBodega = 0;
            try (ResultSet rs = psBodega.getGeneratedKeys()) {
                if (rs.next()) {
                    idBodega = rs.getInt(1);
                }
            }

            // Crear caja automáticamente (puedes cambiar el nombre/ubicación)
            psCaja = con.prepareStatement(sqlCaja);
            psCaja.setString(1, "CAJA - " + b.getNombre());
            psCaja.setString(2, b.getDireccion());
            psCaja.setInt(3, idBodega);
            psCaja.executeUpdate();

            con.commit(); // SUCCESS  Confirmar transacción

        } catch (SQLException e) {
            if (con != null) {
                con.rollback(); // ERROR  Revertir si falla
            }
            throw e;
        } finally {
            if (psBodega != null) {
                psBodega.close();
            }
            if (psCaja != null) {
                psCaja.close();
            }
            if (con != null) {
                con.setAutoCommit(true);
            }
        }
    }

    public void actualizar(ModelBodegas b) throws SQLException {
        String sql = "UPDATE bodegas SET codigo = ?, nombre = ?, direccion = ?, telefono = ?, responsable = ?, tipo = ?, capacidad_maxima = ?, activa = ? "
                   + "WHERE id_bodega = ?";
        try (Connection con = conexion.getInstance().createConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, b.getCodigo());
            ps.setString(2, b.getNombre());
            ps.setString(3, b.getDireccion());
            ps.setString(4, b.getTelefono());
            ps.setString(5, b.getResponsable());
            ps.setString(6, b.getTipo());
            if (b.getCapacidadMaxima() != null) {
                ps.setInt(7, b.getCapacidadMaxima());
            } else {
                ps.setNull(7, java.sql.Types.INTEGER);
            }
            ps.setBoolean(8, b.getActiva() == null ? true : b.getActiva());
            ps.setInt(9, b.getIdBodega());
            ps.executeUpdate();
        }
    }

    public void desactivar(int idBodega) throws SQLException {
        String sql = "UPDATE bodegas SET activa = 0 WHERE id_bodega = ?";
        try (Connection con = conexion.getInstance().createConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idBodega);
            ps.executeUpdate();
        }
    }

    public Integer obtenerIdPorCodigo(String codigo) throws SQLException {
        String sql = "SELECT id_bodega FROM bodegas WHERE codigo = ?";
        try (Connection con = conexion.getInstance().createConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, codigo);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id_bodega");
                }
            }
        }
        return null;
    }

    /**
     * Obtiene una bodega por su ID.
     */
    public ModelBodegas obtenerPorId(int idBodega) throws SQLException {
        String sql = "SELECT id_bodega, codigo, nombre, direccion, telefono, responsable, tipo, capacidad_maxima, activa "
                   + "FROM bodegas WHERE id_bodega = ?";
        try (Connection con = conexion.getInstance().createConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idBodega);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapearBodega(rs);
                }
            }
        }
        return null;
    }

    /**
     * Obtiene una bodega por su código.
     */
    public ModelBodegas obtenerPorCodigo(String codigo) throws SQLException {
        String sql = "SELECT id_bodega, codigo, nombre, direccion, telefono, responsable, tipo, capacidad_maxima, activa "
                   + "FROM bodegas WHERE codigo = ?";
        try (Connection con = conexion.getInstance().createConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, codigo);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapearBodega(rs);
                }
            }
        }
        return null;
    }
}
