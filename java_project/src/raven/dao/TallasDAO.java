package raven.dao;

import java.sql.*;
import raven.controlador.principal.conexion;

public class TallasDAO {
    public Integer findIdTalla(String numero, String sistema, String genero) throws SQLException {
        String sql = "SELECT id_talla FROM tallas WHERE numero=? AND sistema=? AND genero=? AND activo=1 LIMIT 1";
        try (Connection con = conexion.getInstance().createConnection(); PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, numero);
            pst.setString(2, sistema);
            pst.setString(3, genero);
            try (ResultSet rs = pst.executeQuery()) { if (rs.next()) return rs.getInt(1); }
        }
        return null;
    }
}
