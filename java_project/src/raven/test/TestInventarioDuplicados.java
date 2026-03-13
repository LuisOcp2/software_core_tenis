package raven.test;

import java.util.List;
import java.util.HashSet;
import java.util.Set;
import raven.dao.InventarioBodegaDAO;
import raven.application.form.productos.dto.InventarioDetalleItem;
import raven.controlador.principal.conexion;

public class TestInventarioDuplicados {

    public static void main(String[] args) {
        System.out.println("Iniciando prueba de duplicados en inventario...");

        InventarioBodegaDAO dao = new InventarioBodegaDAO();

        // Parámetros de prueba (ajustar según datos reales si es necesario)
        // Usamos null o 0 para traer todo si es posible, o una bodega específica
        Integer idBodega = null;
        String busqueda = "";
        Integer idMarca = 0;
        Integer idCategoria = 0;
        String color = "";
        String talla = "";

        try {
            System.out.println("Consultando InventarioBodegaDAO.buscarInventarioDetallado...");
            List<InventarioDetalleItem> items = dao.buscarInventarioDetallado(
                    idBodega, busqueda, idMarca, idCategoria, color, talla, "", false);

            System.out.println("Total items recuperados: " + items.size());

            Set<String> uniqueKeys = new HashSet<>();
            boolean duplicatesFound = false;

            for (InventarioDetalleItem item : items) {
                // Clave única compuesta por Bodega + Variante
                // Si el item tiene idVariante 0 o null, usamos producto + talla + color como
                // fallback para identificar
                String key = item.getIdBodega() + "-" + item.getIdVariante();

                if (uniqueKeys.contains(key)) {
                    System.out.println("!!! DUPLICADO ENCONTRADO !!!");
                    System.out.println("Bodega: " + item.getNombreBodega() + " (" + item.getIdBodega() + ")");
                    System.out.println("Producto: " + item.getNombreProducto());
                    System.out.println("Variante ID: " + item.getIdVariante());
                    System.out.println("Talla: " + item.getNombreTalla());
                    duplicatesFound = true;
                } else {
                    uniqueKeys.add(key);
                }
            }

            if (!duplicatesFound) {
                System.out.println(">>> PRUEBA EXITOSA: No se encontraron duplicados por (Bodega, Variante ID).");
            } else {
                System.out.println(">>> PRUEBA FALLIDA: Se encontraron duplicados.");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        // Intentar cerrar el pool de conexiones
        try {
            raven.controlador.principal.conexion.getInstance().shutdown();
        } catch (Exception e) {
            // Ignorar
        }
        System.exit(0);
    }
}
