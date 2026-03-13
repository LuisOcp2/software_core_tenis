package raven.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.Statement;
import raven.controlador.principal.conexion;

public class DBUpdater {

    public static void main(String[] args) {
        System.out.println("=== INICIANDO ACTUALIZACIÓN DE BASE DE DATOS ===");
        runUpdates();
        System.out.println("=== ACTUALIZACIÓN COMPLETADA ===");
    }

    public static void runUpdates() {
        // Fixes de esquema
        updateStoredProcedure("updates/fix_variantes_autoincrement.sql");
        updateStoredProcedure("updates/add_status_anulada.sql");
        updateStoredProcedure("updates/add_status_anulada_nc.sql");

        // Stored Procedures
        updateStoredProcedure("updates/sp_buscar_productos_bodega_paginado.sql");
        updateStoredProcedure("updates/sp_listar_variantes_producto.sql");
    }

    public static void updateStoredProcedure(String sqlFilePath) {
        File file = new File(sqlFilePath);

        if (!file.exists()) {
            // Intentar ruta absoluta si relativa falla
            file = new File("c:\\Users\\CrisDEV\\Desktop\\facturas\\zapatosVersion_Xtreme_2_11_25\\zapatosVersion1.0\\"
                    + sqlFilePath.replace("/", "\\"));
        }

        if (!file.exists()) {
            System.err.println("ERROR: No se encontró el archivo SQL: " + sqlFilePath);
            return;
        }

        StringBuilder sql = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                sql.append(line).append("\n");
            }
        } catch (Exception e) {
            System.err.println("ERROR leyendo archivo SQL: " + e.getMessage());
            return;
        }

        String content = sql.toString();
        // Separar DROP y CREATE si están en el mismo archivo
        String[] commands = content.split("(?i)CREATE PROCEDURE");

        String dropCmd = "";
        String createCmd = "";

        if (commands.length > 1) {
            dropCmd = commands[0].trim(); // Lo que está antes de CREATE
            createCmd = "CREATE PROCEDURE " + commands[1].trim(); // Reconstruir CREATE
        } else {
            // Asumir que es solo CREATE o que no se pudo separar
            createCmd = content;
        }

        try (Connection con = conexion.getInstance().createConnection();
                Statement stmt = con.createStatement()) {

            if (!dropCmd.isEmpty()) {
                System.out.println("Ejecutando DROP para: " + sqlFilePath);
                // Limpiar delimitadores si existen (soportar // y $$)
                dropCmd = dropCmd.replace("DELIMITER //", "").replace("DELIMITER ;", "")
                        .replace("DELIMITER $$", "").replace("//", "")
                        .replace("$$", "").replace(";", "");

                if (dropCmd.trim().length() > 0) {
                    try {
                        stmt.execute(dropCmd);
                    } catch (Exception e) {
                        System.err.println("Advertencia al ejecutar DROP (puede que no exista): " + e.getMessage());
                    }
                }
            }

            System.out.println("Ejecutando CREATE para: " + sqlFilePath);
            // Limpiar delimitadores para ejecución JDBC
            // Eliminar DELIMITER //, DELIMITER ; y las marcas de fin // o $$ al final
            String executableCmd = createCmd;

            // Eliminar líneas de DELIMITER
            executableCmd = executableCmd.replaceAll("(?i)DELIMITER\\s+//", "")
                    .replaceAll("(?i)DELIMITER\\s+;", "")
                    .replaceAll("(?i)DELIMITER\\s+\\$\\$", "");

            // Eliminar los delimitadores finales // o $$
            // Nota: Se debe tener cuidado de no eliminar // dentro de strings, pero en SP
            // usualmente están al final
            executableCmd = executableCmd.replaceAll("//\\s*$", "")
                    .replaceAll("\\$\\$\\s*$", "");

            // Eliminar ; final si existe (Statement.execute no siempre lo quiere para un
            // bloque, pero para CREATE PROCEDURE suele ser permisivo o requerirlo sin
            // delimiter)
            // Mejor dejar el ; interno del procedure, y quitar el delimiter final.

            stmt.execute(executableCmd);

            System.out.println("EXITO: Procedimiento " + sqlFilePath + " actualizado correctamente.");

        } catch (Exception e) {
            System.err.println("ERROR ejecutando SQL para " + sqlFilePath + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
}
