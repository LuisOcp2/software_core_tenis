package raven.dao;

import java.sql.*;
import raven.controlador.principal.conexion;

public class UsuariosDAO {
    public String getUbicacionById(int idUsuario) throws SQLException {
        String sql = "SELECT ubicacion FROM usuarios WHERE id_usuario = ?";
        try (Connection con = conexion.getInstance().createConnection(); PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setInt(1, idUsuario);
            try (ResultSet rs = pst.executeQuery()) { if (rs.next()) return rs.getString(1); }
        }
        return null;
    }

    public int findFirstActiveUserId() throws SQLException {
        String sql = "SELECT id_usuario FROM usuarios WHERE activo = 1 ORDER BY id_usuario ASC LIMIT 1";
        try (Connection con = conexion.getInstance().createConnection(); PreparedStatement pst = con.prepareStatement(sql); ResultSet rs = pst.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        }
        return -1;
    }
}
