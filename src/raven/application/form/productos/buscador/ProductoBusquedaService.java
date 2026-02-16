package raven.application.form.productos.buscador;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import javax.swing.ImageIcon;
import raven.controlador.principal.conexion;

public class ProductoBusquedaService {

    public List<ProductoBusquedaItem> buscarProductos(String query) {
        List<ProductoBusquedaItem> lista = new ArrayList<>();

        // Usar búsqueda más eficiente con índices
        StringBuilder sql = new StringBuilder(
            "SELECT DISTINCT pv.id_variante, pv.ean, p.nombre, t.numero AS talla, p.genero, " +
            "COALESCE(ib.Stock_par, 0) as stock_pares, COALESCE(ib.Stock_caja, 0) as stock_cajas, " +
            "b.nombre as nombre_bodega, m.nombre as marca, c.nombre as color, p.id_producto " +
            "FROM productos p " +
            "LEFT JOIN producto_variantes pv ON pv.id_producto = p.id_producto AND pv.disponible = 1 " +
            "LEFT JOIN tallas t ON pv.id_talla = t.id_talla " +
            "LEFT JOIN colores c ON pv.id_color = c.id_color " +
            "LEFT JOIN marcas m ON p.id_marca = m.id_marca " +
            "LEFT JOIN inventario_bodega ib ON pv.id_variante = ib.id_variante AND ib.activo = 1 " +
            "LEFT JOIN bodegas b ON ib.id_bodega = b.id_bodega " +
            "WHERE p.activo = 1 ");

        List<Object> params = new ArrayList<>();

        if (query != null && !query.trim().isEmpty()) {
            String[] parts = query.split(",");

            // Parte 1: Nombre/General (Nombre Producto, Marca o EAN)
            if (parts.length > 0 && !parts[0].trim().isEmpty()) {
                String p1 = "%" + parts[0].trim() + "%";
                sql.append(" AND (p.nombre LIKE ? OR m.nombre LIKE ? OR pv.ean LIKE ?) ");
                params.add(p1);
                params.add(p1);
                params.add(p1);
            }

            // Parte 2: Color (Opcional)
            if (parts.length > 1 && !parts[1].trim().isEmpty()) {
                String p2 = "%" + parts[1].trim() + "%";
                sql.append(" AND (c.nombre LIKE ?) ");
                params.add(p2);
            }

            // Parte 3: Talla (Opcional)
            if (parts.length > 2 && !parts[2].trim().isEmpty()) {
                String p3 = "%" + parts[2].trim() + "%";
                sql.append(" AND (t.numero LIKE ?) ");
                params.add(p3);
            }
        }

        sql.append("ORDER BY p.nombre ASC " +
                   "LIMIT 100");

        try (Connection con = conexion.getInstance().createConnection();
             PreparedStatement ps = con.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int stockPares = rs.getInt("stock_pares");
                    int stockCajas = rs.getInt("stock_cajas");
                    int stock = stockPares > 0 ? stockPares : stockCajas;
                    String tipo = stockPares > 0 ? "Pares" : "Cajas";

                    lista.add(new ProductoBusquedaItem(
                        rs.getInt("id_variante"),
                        rs.getString("ean"),
                        rs.getString("nombre"),
                        rs.getString("talla"),
                        rs.getString("genero"),
                        stock,
                        tipo,
                        rs.getString("nombre_bodega"),
                        rs.getString("marca"),
                        rs.getString("color"),
                        rs.getInt("id_producto")
                    ));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return lista;
    }

    public ImageIcon obtenerImagenVariante(int idVariante) {
        String sql = "SELECT imagen FROM producto_variantes WHERE id_variante = ?";
        try (Connection con = conexion.getInstance().createConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idVariante);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    byte[] imgData = rs.getBytes("imagen");
                    if (imgData != null && imgData.length > 0) {
                        return new ImageIcon(imgData);
                    }
                }
            }
        } catch (Exception e) {
            // Silencioso en carga de imagenes
        }
        return null;
    }
}
