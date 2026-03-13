package raven.clases.inventario;

import java.sql.SQLException;
import java.util.List;
import raven.clases.productos.ServiceProduct;
import raven.controlador.productos.ModelProduct;
import raven.dao.ConteoInventarioDAO;
import raven.dao.ProductoDAO;
import raven.modelos.ConteoInventario;
import raven.modelos.DetalleConteoInventario;
import raven.dao.InventarioBodegaDAO;
import raven.application.form.productos.dto.InventarioDetalleItem;
import java.util.ArrayList;
import raven.controlador.productos.ModelBrand;
import raven.controlador.productos.ModelCategory;

/**
 * Servicio para la gestión de conteos de inventario. Coordina las operaciones
 * de negocio relacionadas con conteos y ajustes.
 */
public class ServiceConteoInventario {

    private final ConteoInventarioDAO conteoDAO;
    private final ProductoDAO productoDAO;
    private final ServiceProduct serviceProduct;
    private final InventarioBodegaDAO inventarioBodegaDAO;

    public ServiceConteoInventario() {
        this.conteoDAO = new ConteoInventarioDAO();
        this.productoDAO = new ProductoDAO();
        this.serviceProduct = new ServiceProduct();
        this.inventarioBodegaDAO = new InventarioBodegaDAO();
    }

    /**
     * Obtiene la lista de conteos activos (pendientes o en proceso)
     *
     * @param tipo Tipo de conteo (cajas o pares)
     * @return Lista de conteos activos
     * @throws SQLException Si ocurre un error de base de datos
     */
    public List<ConteoInventario> obtenerConteosActivos(String tipo) throws SQLException {
        return conteoDAO.obtenerConteosActivos(tipo);
    }

    /**
     * Cierra un conteo parcialmente completado
     *
     * @param idConteo  ID del conteo
     * @param usuarioId ID del usuario que cierra el conteo
     * @return true si se cerró correctamente, false en caso contrario
     * @throws SQLException Si ocurre un error de base de datos
     */
    public boolean cerrarConteoParcial(int idConteo, int usuarioId) throws SQLException {
        // Cambiar el estado del conteo a "completado"
        System.out.println("desde ServiceConteoInventario: " + idConteo);

        if (conteoDAO.actualizarEstadoConteo(idConteo, "completado")) {
            // Generar ajustes automáticos solo para productos ya contados
            List<DetalleConteoInventario> detalles = conteoDAO.obtenerDetallesContados(idConteo);

            for (DetalleConteoInventario detalle : detalles) {
                // Solo crear ajustes para productos con diferencia
                if (detalle.getDiferencia() != 0) {
                    conteoDAO.crearAjusteAutomatico(detalle, usuarioId);
                }
            }

            return true;
        }

        return false;
    }

    /**
     * Obtiene los detalles de un conteo específico
     *
     * @param idConteo ID del conteo
     * @return Lista de detalles del conteo
     * @throws SQLException Si ocurre un error de base de datos
     */
    public List<DetalleConteoInventario> obtenerDetallesConteo(int idConteo) throws SQLException {
        return conteoDAO.obtenerDetallesConteo(idConteo);
    }

    /**
     * Obtiene los productos disponibles para incluir en un conteo
     *
     * @param tipoCajas true si es conteo de cajas, false si es de pares
     * @return Lista de productos disponibles
     * @throws SQLException Si ocurre un error de base de datos
     */
    public List<ModelProduct> obtenerProductosParaConteo(boolean tipoCajas) throws SQLException {
        String vistaInventario = tipoCajas ? "vista_inventario_cajas" : "vista_inventario_pares";
        return productoDAO.obtenerProductosPorVista(vistaInventario);
    }

    /**
     * Busca productos por término de búsqueda para incluir en un conteo
     *
     * @param termino   Término de búsqueda
     * @param tipoCajas true si es conteo de cajas, false si es de pares
     * @return Lista de productos que coinciden con la búsqueda
     * @throws SQLException Si ocurre un error de base de datos
     */
    public List<ModelProduct> buscarProductosParaConteo(String termino, boolean tipoCajas) throws SQLException {
        String vistaInventario = tipoCajas ? "vista_inventario_cajas" : "vista_inventario_pares";
        return productoDAO.buscarProductosPorVista(termino, vistaInventario);
    }

    public List<ModelProduct> obtenerProductosParaConteoBodega(boolean tipoCajas, int idBodega, String ubicacionFiltro,
            String termino, int idMarca, int idCategoria) throws SQLException {
        // Use InventarioBodegaDAO to get items for this bodega
        // If termino is null, pass empty string
        if (termino == null)
            termino = "";

        List<InventarioDetalleItem> items = inventarioBodegaDAO.buscarInventarioDetallado(
                idBodega, termino, idMarca, idCategoria, "", "", "", false); // Using idMarca and idCategoria

        List<ModelProduct> products = new ArrayList<>();
        String ubicacionBuscada = (ubicacionFiltro != null) ? ubicacionFiltro.toLowerCase().trim() : "";

        for (InventarioDetalleItem item : items) {
            // Filter by ubicacion if provided
            if (!ubicacionBuscada.isEmpty()) {
                String ubi = item.getUbicacion();
                if (ubi == null || !ubi.trim().equalsIgnoreCase(ubicacionBuscada)) {
                    continue;
                }
            }

            // Filter by logic of "has stock" or "exists"?
            // Normally create count might want all items even with 0 stock?
            // buscarInventarioDetallado usually returns items that exist in
            // inventario_bodega.

            ModelProduct p = new ModelProduct();
            p.setProductId(item.getIdProducto());
            p.setVariantId(item.getIdVariante()); // Set variant ID
            p.setName(item.getNombreProducto());
            p.setBarcode(item.getEan() != null && !item.getEan().isEmpty() ? item.getEan() : item.getCodigoModelo());
            // IMPORTANT: We need to store the variant info because count works on variants?
            // The existing ModelProduct mapping in DAO usually flattened variants.
            // Here we just map what we have.
            p.setSize(item.getNombreTalla());
            p.setColor(item.getNombreColor());

            ModelCategory cat = new ModelCategory();
            cat.setName(item.getCategoria());
            p.setCategory(cat);

            ModelBrand brand = new ModelBrand();
            brand.setName(item.getNombreMarca());
            p.setBrand(brand);

            p.setPairsStock(item.getStockPares());
            p.setBoxesStock(item.getStockCajas());

            // We use a custom field or reuse one for the specific location if needed in the
            // UI?
            // For now just filtering was requested.

            products.add(p);
        }
        return products;
    }

    /**
     * Verifica si un conteo puede cerrarse (todos los productos contados)
     *
     * @param idConteo ID del conteo
     * @return true si el conteo puede cerrarse, false en caso contrario
     * @throws SQLException Si ocurre un error de base de datos
     */
    public boolean verificarConteoCompleto(int idConteo) throws SQLException {
        int pendientes = conteoDAO.contarProductosPendientes(idConteo);
        return pendientes == 0;
    }

    /**
     * Cierra un conteo y genera los ajustes necesarios
     *
     * @param idConteo  ID del conteo
     * @param usuarioId ID del usuario que cierra el conteo
     * @return true si se cerró correctamente, false en caso contrario
     * @throws SQLException Si ocurre un error de base de datos
     */
    public boolean cerrarConteo(int idConteo, int usuarioId) throws SQLException {
        // Verificar si todos los productos están contados
        System.out.println("desde WXYZ: " + idConteo);
        if (idConteo <= 0) {
            throw new SQLException("ID de conteo inválido: " + idConteo);
        }
        if (!verificarConteoCompleto(idConteo)) {
            return false;
        }

        // Cambiar el estado del conteo a "completado"
        boolean statusUpdated = conteoDAO.actualizarEstadoConteo(idConteo, "completado");
        System.out.println("DEBUG SERVICE: Status Updated Result: " + statusUpdated);

        if (statusUpdated) {
            // Generar ajustes automáticos para productos con diferencias
            List<DetalleConteoInventario> detalles = conteoDAO.obtenerDetallesConDiferencias(idConteo);
            System.out.println(
                    "DEBUG: Conteo ID " + idConteo + " - Detalles con diferencias encontrados: " + detalles.size());

            for (DetalleConteoInventario detalle : detalles) {
                System.out.println("DEBUG: Creando ajuste para detalle ID: " + detalle.getId() + ", Dif: "
                        + detalle.getDiferencia());
                boolean created = conteoDAO.crearAjusteAutomatico(detalle, usuarioId);
                System.out.println("DEBUG: Ajuste creado: " + created);
            }

            return true;
        } else {
            System.out.println("DEBUG SERVICE: Failed to update status to completado.");
        }

        return false;
    }
}
