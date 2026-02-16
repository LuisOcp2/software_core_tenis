package raven.clases.principal;

import raven.controlador.principal.conexion;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.SQLException;

public class DBMigration {
    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Uso: java DBMigration <archivo_sql>");
            return;
        }

        String sqlFile = args[0];
        System.out.println("Ejecutando script: " + sqlFile);

        StringBuilder sql = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(sqlFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                sql.append(line).append("\n");
            }
        } catch (IOException e) {
            System.err.println("Error leyendo archivo: " + e.getMessage());
            return;
        }

        try (Connection conn = conexion.getInstance().getConnection();
             Statement stmt = conn.createStatement()) {
            
            String content = sql.toString();
            String[] commands;
            // Check for custom delimiter $$
            if (content.contains("$$")) {
                commands = content.split("\\$\\$");
            } else {
                commands = content.split(";");
            }

            for (String command : commands) {
                String trimmed = command.trim();
                if (trimmed.isEmpty()) continue;
                
                // Remove trailing semicolon if present (common issue with some drivers)
                if (trimmed.endsWith(";")) {
                    trimmed = trimmed.substring(0, trimmed.length() - 1).trim();
                }

                try {
                    stmt.execute(trimmed);
                    System.out.println("Ejecutado: " + trimmed.substring(0, Math.min(trimmed.length(), 50)) + "...");
                } catch (SQLException e) {
                    System.err.println("Error ejecutando comando: " + e.getMessage());
                    System.err.println("SQL: " + trimmed);
                }
            }
            System.out.println("Migración completada.");
        } catch (SQLException e) {
            System.err.println("Error de conexión: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
