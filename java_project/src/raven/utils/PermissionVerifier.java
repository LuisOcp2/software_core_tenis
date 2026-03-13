package raven.utils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import raven.controlador.principal.conexion;

public class PermissionVerifier {
    public static void main(String[] args) {
        String[] modulos = {
                "productos_info",
                "productos_variantes",
                "variante_stock",
                "variante_editar",
                "variante_eliminar"
        };

        System.out.println("=== VERIFICACIÓN DE PERMISOS INSERTADOS ===");
        System.out.println(String.format("%-5s | %-20s | %-30s | %-15s", "ID", "MODULO", "NOMBRE", "CATEGORIA"));
        System.out.println("--------------------------------------------------------------------------------");

        try (Connection con = conexion.getInstance().createConnection()) {
            if (con == null) {
                System.out.println("ERROR: No se pudo conectar a la base de datos.");
                return;
            }

            String sql = "SELECT id_permiso, modulo, nombre_mostrar, categoria FROM permisos WHERE modulo = ?";

            for (String modulo : modulos) {
                try (PreparedStatement ps = con.prepareStatement(sql)) {
                    ps.setString(1, modulo);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            System.out.println(String.format("%-5d | %-20s | %-30s | %-15s",
                                    rs.getInt("id_permiso"),
                                    rs.getString("modulo"),
                                    rs.getString("nombre_mostrar"),
                                    rs.getString("categoria")));
                        } else {
                            System.out.println(String.format("%-5s | %-20s | %-30s | %-15s",
                                    "MISS", modulo, "NO ENCONTRADO", "---"));
                        }
                    }
                }
            }
            System.out.println("============================================");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
