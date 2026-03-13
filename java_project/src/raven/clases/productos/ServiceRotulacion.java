package raven.clases.productos;

import com.google.zxing.*;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import raven.controlador.principal.conexion;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.sql.*;
import java.io.File;

public class ServiceRotulacion {

    public static void main(String[] args) {
        try {
            // Conexión a base de datos usando variables de conexion
            String url = "jdbc:mysql://" + conexion.host + ":" + conexion.port + "/" + conexion.database;
            Connection conn = DriverManager.getConnection(url, conexion.username, conexion.password);
            
            String query = "SELECT p.nombre, COALESCE(t.numero, '') AS talla, p.precio_venta, " +
                          "COALESCE(pv.ean, p.codigo_modelo) AS codigo_barras " +
                          "FROM productos p " +
                          "LEFT JOIN producto_variantes pv ON p.id_producto = pv.id_producto " +
                          "LEFT JOIN tallas t ON pv.id_talla = t.id_talla " +
                          "WHERE p.id_producto = 9";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(query);

            if (rs.next()) {
                String nombre = rs.getString("nombre");
                String talla = rs.getString("talla");
                String precio = rs.getString("precio_venta");
                String codigoBarras = rs.getString("codigo_barras");

                mostrarEtiqueta(nombre, talla, precio, codigoBarras);
            }

            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void mostrarEtiqueta(String nombre, String talla, String precio, String codigoBarras) {
        JFrame frame = new JFrame("Etiqueta de Producto");
        frame.setSize(420, 250);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel panel = new JPanel() {
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);

                // Simulación de logo (puedes usar drawImage con una imagen real)
                g.setFont(new Font("Arial", Font.BOLD, 14));
                g.drawString("LOGO EMPRESA", 10, 25);
                // Datos del producto
                g.setFont(new Font("Arial", Font.PLAIN, 12));
                g.drawString("Modelo: " + nombre, 140, 25);
                g.drawString("Talla: " + talla, 140, 45);
                g.drawString("Precio: $" + precio, 140, 65);

                try {
                    BufferedImage barcode = generarCodigoBarras(codigoBarras);
                    g.drawImage(barcode, 90, 90, null);
                    g.drawString("EAN-13: " + codigoBarras, 130, 165);
                } catch (Exception e) {
                    g.drawString("Error generando código de barras", 90, 120);
                }
            }
        };

        frame.add(panel);
        frame.setVisible(true);
    }

    public static BufferedImage generarCodigoBarras(String datos) throws Exception {
        if (datos.length() < 13) {
            throw new Exception("El código debe tener 13 dígitos para EAN-13");
        }

        BitMatrix matrix = new MultiFormatWriter().encode(datos, BarcodeFormat.EAN_13, 200, 60);
        return MatrixToImageWriter.toBufferedImage(matrix);
    }
}
