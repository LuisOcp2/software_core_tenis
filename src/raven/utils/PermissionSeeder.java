package raven.utils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import raven.controlador.principal.conexion;

public class PermissionSeeder {
    public static void main(String[] args) {
        String[] permisos = {
                "productos_info", "Editar Información General", "Inventario",
                "productos_variantes", "Ver/Gestionar Variantes", "Inventario",
                "variante_stock", "Modificar Stock Manual", "Inventario",
                "variante_editar", "Editar Variante", "Inventario",
                "variante_eliminar", "Eliminar Variante", "Inventario"
        };

        try (Connection con = conexion.getInstance().createConnection()) {
            if (con == null) {
                System.out.println("ERROR: No se pudo conectar a la base de datos.");
                return;
            }

            String checkSql = "SELECT id_permiso FROM permisos WHERE modulo = ?";
            String insertSql = "INSERT INTO permisos (modulo, nombre_mostrar, categoria, descripcion, requiere_admin, activo) VALUES (?, ?, ?, ?, 0, 1)";

            for (int i = 0; i < permisos.length; i += 3) {
                String modulo = permisos[i];
                String nombre = permisos[i + 1];
                String categoria = permisos[i + 2];

                // Verificar si existe
                try (PreparedStatement psCheck = con.prepareStatement(checkSql)) {
                    psCheck.setString(1, modulo);
                    try (ResultSet rs = psCheck.executeQuery()) {
                        if (rs.next()) {
                            System.out.println("El permiso '" + modulo + "' ya existe. Saltando.");
                        } else {
                            // Insertar
                            try (PreparedStatement psInsert = con.prepareStatement(insertSql)) {
                                psInsert.setString(1, modulo);
                                psInsert.setString(2, nombre);
                                psInsert.setString(3, categoria);
                                psInsert.setString(4, "Permiso para " + nombre);
                                psInsert.executeUpdate();
                                System.out.println("SUCCESS: Permiso '" + modulo + "' creado.");
                            }
                        }
                    }
                }
            }
            System.out.println("Proceso completado.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
