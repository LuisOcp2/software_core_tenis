/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package raven.controlador.inventario;

import java.awt.Color;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import raven.clases.inventario.ServiceConteoInventario;
import raven.controlador.productos.ModelProduct;
import raven.dao.ConteoInventarioDAO;
import raven.modelos.ConteoInventario;
import raven.modelos.DetalleConteoInventario;
import raven.modelos.Usuario;

/**
 * Controlador para gestionar las operaciones de inventario. Coordina entre la
 * interfaz gráfica y la lógica de negocio.
 */
public class InventarioController {

    private final ServiceConteoInventario serviceConteo;
    private final ConteoInventarioDAO conteoDAO;

    public InventarioController() {
        this.serviceConteo = new ServiceConteoInventario();
        this.conteoDAO = new ConteoInventarioDAO();
    }

    /**
     * Carga los conteos activos de inventario en una tabla
     *
     * @param tabla     La tabla donde se mostrarán los conteos
     * @param tipoCajas true si son conteos de cajas, false si son de pares
     */
    public void cargarConteosActivos(JTable tabla, boolean tipoCajas) {
        try {
            String tipo = tipoCajas ? "cajas" : "pares";
            List<ConteoInventario> conteos = conteoDAO.obtenerConteosActivos(tipo);

            DefaultTableModel modelo = (DefaultTableModel) tabla.getModel();
            modelo.setRowCount(0);
            // Ocultar conteos ya completados en la vista de "Activos"
            for (ConteoInventario conteo : conteos) {
                if (conteo.getEstado() != null && conteo.getEstado().equalsIgnoreCase("completado")) {
                    continue;
                }
                Object[] fila = {
                        conteo.getId(),
                        conteo.getNombre(),
                        new SimpleDateFormat("dd/MM/yyyy").format(conteo.getFechaProgramada()),
                        conteo.getTipoConteo(),
                        conteo.getResponsable().getNombre(),
                        conteo.getObservaciones(),
                        conteo.getEstado(),
                        "Ver" // Para el botón de acción
                };
                modelo.addRow(fila);
                ajustarColumnasTabla(tabla);
            }

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null,
                    "Error al cargar conteos: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Carga los ajustes de inventario pendientes en una tabla
     *
     * @param tabla     La tabla donde se mostrarán los ajustes
     * @param tipoCajas true si son ajustes de cajas, false si son de pares
     */
    public void cargarAjustesPendientes(JTable tabla, boolean tipoCajas) {
        try {
            String tipo = tipoCajas ? "cajas" : "pares";
            DefaultTableModel modelo = (DefaultTableModel) tabla.getModel();
            modelo.setRowCount(0);

            try {
                List<DetalleConteoInventario> ajustes = conteoDAO.obtenerAjustesPendientes(tipo);

                if (ajustes.isEmpty()) {
                    System.out.println("No se encontraron ajustes pendientes para el tipo: " + tipo);
                    return;
                }

                for (DetalleConteoInventario ajuste : ajustes) {
                    // Verificar que el producto no sea nulo
                    if (ajuste.getProducto() == null) {
                        System.out.println("Advertencia: Producto nulo en ajuste ID: " + ajuste.getIdAjuste());
                        continue;
                    }

                    // Añadir una columna oculta para el ID del ajuste
                    Object[] row = {
                            ajuste.getProducto().getBarcode(),
                            ajuste.getProducto().getName(),
                            ajuste.getStockSistema(),
                            ajuste.getStockContado(),
                            ajuste.getDiferencia(),
                            obtenerTipoAjuste(ajuste.getDiferencia(), tipoCajas),
                            ajuste.getRazonAjuste(), // Razón del ajuste como string
                            Boolean.FALSE, // Checkbox para aprobar (como Boolean, no String)
                            ajuste.getIdAjuste() // Columna oculta para el ID
                    };
                    modelo.addRow(row);
                }

            } catch (SQLException e) {
                System.err.println("Error SQL al obtener ajustes: " + e.getMessage());
                e.printStackTrace();
                JOptionPane.showMessageDialog(null,
                        "Error al cargar ajustes: " + e.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception e) {
            System.err.println("Error general al cargar ajustes: " + e.getMessage());
            e.printStackTrace();
            JOptionPane.showMessageDialog(null,
                    "Error inesperado al cargar ajustes: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Crea un nuevo conteo de inventario
     *
     * @param nombre                 Nombre del conteo
     * @param tipoConteo             Tipo de conteo (general, parcial, etc.)
     * @param fecha                  Fecha programada
     * @param hora                   Hora programada
     * @param responsableId          ID del usuario responsable
     * @param tipoCajas              true si es conteo de cajas, false si es de
     *                               pares
     * @param observaciones          Observaciones adicionales
     * @param productosSeleccionados Lista de productos a incluir en el conteo
     * @return true si se creó correctamente, false en caso contrario
     */
    public boolean crearConteoInventario(String nombre, String tipoConteo,
            Date fecha, String hora, int responsableId, boolean tipoCajas,
            String observaciones, List<ModelProduct> productosSeleccionados, int idBodega) {

        if (productosSeleccionados.isEmpty()) {
            JOptionPane.showMessageDialog(null,
                    "Debe seleccionar al menos un producto válido para el conteo.",
                    "Validación", JOptionPane.WARNING_MESSAGE);
            return false;
        }

        try {
            // Verificar que todos los productos tengan ID válido
            for (ModelProduct producto : productosSeleccionados) {
                if (producto.getProductId() <= 0) {
                    JOptionPane.showMessageDialog(null,
                            "El producto " + producto.getName() + " no tiene un ID válido.",
                            "Error", JOptionPane.ERROR_MESSAGE);
                    return false;
                }
            }

            // Crear el objeto de conteo
            ConteoInventario conteo = new ConteoInventario();
            conteo.setNombre(nombre);
            conteo.setTipoConteo(tipoConteo);
            conteo.setFechaProgramada(fecha);
            conteo.setHoraProgramada(hora);
            conteo.setIdBodega(idBodega);

            Usuario responsable = new Usuario();
            responsable.setId(responsableId);
            conteo.setResponsable(responsable);

            conteo.setTipo(tipoCajas ? "cajas" : "pares");
            conteo.setEstado("pendiente");
            conteo.setObservaciones(observaciones);
            conteo.setPrioridad("media"); // Por defecto

            // Crear el conteo en la base de datos
            int idConteo = conteoDAO.crearConteoInventario(conteo);

            if (idConteo > 0) {
                // Añadir los productos al conteo
                List<DetalleConteoInventario> detalles = new ArrayList<>();

                for (ModelProduct producto : productosSeleccionados) {
                    DetalleConteoInventario detalle = new DetalleConteoInventario();
                    detalle.setIdConteo(idConteo);
                    detalle.setProducto(producto);
                    detalle.setStockSistema(tipoCajas ? producto.getBoxesStock() : producto.getPairsStock());
                    detalle.setEstado("pendiente");
                    detalles.add(detalle);
                }

                // Guardar los detalles
                boolean result = conteoDAO.agregarDetallesConteo(detalles);
                if (!result) {
                    // Si falla la creación de detalles, eliminar el conteo principal
                    conteoDAO.eliminarConteo(idConteo);
                    JOptionPane.showMessageDialog(null,
                            "Error al agregar productos al conteo. El conteo ha sido eliminado.",
                            "Error", JOptionPane.ERROR_MESSAGE);
                    return false;
                }

                return true;
            }

            return false;

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null,
                    "Error al crear conteo: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    /**
     * Obtiene los detalles de un conteo específico
     *
     * @param idConteo ID del conteo
     * @return Lista de detalles del conteo
     * @throws SQLException Si ocurre un error de base de datos
     */
    public List<DetalleConteoInventario> obtenerDetallesConteo(int idConteo) throws SQLException {
        try {
            return conteoDAO.obtenerDetallesConteo(idConteo);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null,
                    "Error al obtener detalles del conteo: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            throw e;
        }
    }

    /**
     * Obtiene los productos con diferencias en un conteo específico
     *
     * @param idConteo ID del conteo
     * @return Lista de detalles con diferencias
     * @throws SQLException Si ocurre un error de base de datos
     */
    public List<DetalleConteoInventario> obtenerDetallesConDiferencias(int idConteo) throws SQLException {
        try {
            return conteoDAO.obtenerDetallesConDiferencias(idConteo);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null,
                    "Error al obtener diferencias: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            throw e;
        }
    }

    /**
     * Cierra un conteo y genera los ajustes necesarios
     *
     * @param idConteo  ID del conteo
     * @param usuarioId ID del usuario que cierra el conteo
     * @return true si se cerró correctamente, false en caso contrario
     */
    public boolean cerrarConteo(int idConteo, int usuarioId) {
        System.out.println("Cerrando conteo con ID desde Inventario: " + idConteo);

        try {
            // Verificar si todos los productos están contados
            if (!serviceConteo.verificarConteoCompleto(idConteo)) {
                // Si hay productos pendientes, preguntar confirmar
                System.out.println("desde aqui if 12345: " + idConteo);

                return serviceConteo.cerrarConteoParcial(idConteo, usuarioId);
            } else {
                System.out.println("desde aqui if 34567: " + idConteo);
                // Si todos están contados, cerrar normalmente
                return serviceConteo.cerrarConteo(idConteo, usuarioId);
            }
        } catch (SQLException e) {
            System.err.println("ERROR CRITICO en cerrarConteo:");
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Busca detalles de conteo por término de búsqueda
     *
     * @param idConteo ID del conteo
     * @param termino  Término de búsqueda
     * @return Lista de detalles que coinciden con la búsqueda
     * @throws SQLException Si ocurre un error de base de datos
     */
    public List<DetalleConteoInventario> buscarDetallesConteo(int idConteo, String termino) throws SQLException {
        try {
            return conteoDAO.buscarDetallesConteo(idConteo, termino);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null,
                    "Error al buscar detalles de conteo: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            throw e;
        }
    }

    /**
     * Busca conteos activos por término de búsqueda
     *
     * @param tabla   Tabla donde se mostrarán los resultados
     * @param termino Término de búsqueda
     * @param esCajas true si es búsqueda de cajas, false si es de pares
     */
    public void buscarConteosActivos(JTable tabla, String termino, boolean esCajas) {
        try {
            // Obtener el tipo de conteo según el parámetro
            String tipo = esCajas ? "cajas" : "pares";

            // Obtener el modelo de la tabla
            DefaultTableModel modelo = (DefaultTableModel) tabla.getModel();
            modelo.setRowCount(0); // Limpiar tabla

            // Obtener los conteos filtrados
            List<ConteoInventario> conteos = obtenerConteosActivosFiltrados(termino, tipo);

            for (ConteoInventario conteo : conteos) {
                Object[] fila = {
                        conteo.getId(),
                        conteo.getNombre(),
                        new SimpleDateFormat("dd/MM/yyyy").format(conteo.getFechaProgramada()),
                        conteo.getTipoConteo(),
                        conteo.getResponsable().getNombre(),
                        conteo.getObservaciones(),
                        conteo.getEstado(),
                        "Ver" // Para el botón de acción
                };
                modelo.addRow(fila);
                ajustarColumnasTabla(tabla);
            }
        } catch (SQLException e) {
            // Registrar el error
            Logger.getLogger(InventarioController.class.getName()).log(Level.SEVERE, null, e);
            throw new RuntimeException("Error al buscar conteos: " + e.getMessage());
        }
    }

    /**
     * Ajusta la configuración de las columnas de la tabla de conteos
     *
     * @param tabla Tabla a ajustar
     */
    private void ajustarColumnasTabla(JTable tabla) {

        // Establecer anchos preferidos para las demás columnas
        tabla.getColumnModel().getColumn(1).setPreferredWidth(150); // Nombre conteo
        tabla.getColumnModel().getColumn(2).setPreferredWidth(100); // Fecha
        tabla.getColumnModel().getColumn(3).setPreferredWidth(80); // Estado
        tabla.getColumnModel().getColumn(4).setPreferredWidth(120); // Responsable
        tabla.getColumnModel().getColumn(5).setPreferredWidth(150); // Observaciones
        tabla.getColumnModel().getColumn(6).setPreferredWidth(80); // Estado (visualización)
        tabla.getColumnModel().getColumn(7).setPreferredWidth(80); // Acciones

        // Personalizar renderizador para la columna de acciones (botón "Ver")
        tabla.getColumnModel().getColumn(7).setCellRenderer(
                new javax.swing.table.DefaultTableCellRenderer() {
                    @Override
                    public java.awt.Component getTableCellRendererComponent(JTable table, Object value,
                            boolean isSelected, boolean hasFocus, int row, int column) {
                        javax.swing.JButton btn = new javax.swing.JButton("Ver");
                        btn.setBackground(new Color(10, 132, 255));
                        btn.setForeground(Color.WHITE);
                        btn.setBorderPainted(false);
                        return btn;
                    }
                });

        // Opcional: Personalizar renderizador para la columna de estado
        tabla.getColumnModel().getColumn(6).setCellRenderer(
                new javax.swing.table.DefaultTableCellRenderer() {
                    @Override
                    public java.awt.Component getTableCellRendererComponent(JTable table, Object value,
                            boolean isSelected, boolean hasFocus, int row, int column) {
                        javax.swing.JLabel label = new javax.swing.JLabel();
                        if (value != null) {
                            label.setText(value.toString());

                            // Aplicar colores según el estado
                            switch (value.toString().toLowerCase()) {
                                case "pendiente":
                                    label.setForeground(new Color(255, 150, 0)); // Naranja
                                    break;
                                case "en proceso":
                                    label.setForeground(new Color(10, 132, 255)); // Azul
                                    break;
                                case "completado":
                                    label.setForeground(new Color(40, 205, 65)); // Verde
                                    break;
                                case "anulado":
                                    label.setForeground(Color.RED); // Rojo
                                    break;
                                default:
                                    label.setForeground(table.getForeground());
                                    break;
                            }

                            label.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
                        }
                        return label;
                    }
                });
    }

    /**
     * Obtiene conteos activos filtrados por un término
     *
     * @param termino Término de búsqueda
     * @param tipo    Tipo de conteo (cajas o pares)
     * @return Lista de conteos filtrados
     * @throws SQLException Si ocurre un error de base de datos
     */
    private List<ConteoInventario> obtenerConteosActivosFiltrados(String termino, String tipo) throws SQLException {
        // Usar el DAO para obtener los conteos
        ConteoInventarioDAO dao = new ConteoInventarioDAO();
        List<ConteoInventario> todosLosConteos = dao.obtenerConteosActivos(tipo);

        // Excluir conteos completados de la lista base
        List<ConteoInventario> conteosSinCompletados = todosLosConteos.stream()
                .filter(c -> c.getEstado() == null || !c.getEstado().equalsIgnoreCase("completado"))
                .collect(Collectors.toList());

        // Filtrar por término (si está vacío, retornar todos)
        if (termino.isEmpty()) {
            return conteosSinCompletados;
        }

        // Convertir a minúsculas para búsqueda insensible a mayúsculas/minúsculas
        String terminoLower = termino.toLowerCase();

        // Filtrar conteos que coincidan con el término en nombre, responsable, estado u
        // observaciones
        return conteosSinCompletados.stream()
                .filter(c -> c.getNombre().toLowerCase().contains(terminoLower)
                        || c.getResponsable().getNombre().toLowerCase().contains(terminoLower)
                        || c.getEstado().toLowerCase().contains(terminoLower)
                        || (c.getObservaciones() != null && c.getObservaciones().toLowerCase().contains(terminoLower)))
                .collect(Collectors.toList());
    }

    /**
     * Busca el ID de un ajuste por su código de producto
     *
     * @param codigo  Código de barras del producto
     * @param esCajas true si es ajuste de cajas, false si es de pares
     * @return ID del ajuste encontrado
     * @throws SQLException Si ocurre un error de base de datos
     */
    public int buscarIdAjustePorCodigo(String codigo, boolean esCajas) throws SQLException {
        String tipo = esCajas ? "cajas" : "pares";
        List<DetalleConteoInventario> ajustes = conteoDAO.obtenerAjustesPendientes(tipo);

        for (DetalleConteoInventario ajuste : ajustes) {
            if (ajuste.getProducto() != null
                    && ajuste.getProducto().getBarcode() != null
                    && ajuste.getProducto().getBarcode().equals(codigo)) {
                return ajuste.getIdAjuste();
            }
        }

        throw new SQLException("No se encontró ajuste con código: " + codigo);
    }

    /**
     * Registra el conteo real para un producto específico
     *
     * @param idDetalleConteo   ID del detalle de conteo
     * @param stockContado      Cantidad contada
     * @param usuarioContadorId ID del usuario que realizó el conteo
     * @return true si se registró correctamente, false en caso contrario
     */
    public boolean registrarConteoProducto(int idDetalleConteo, int stockContado, int usuarioContadorId) {
        try {
            return conteoDAO.actualizarStockContado(idDetalleConteo, stockContado, usuarioContadorId);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null,
                    "Error al registrar conteo: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    /**
     * Aprueba los ajustes de inventario seleccionados
     *
     * @param ajustesSeleccionados Lista de ajustes aprobados
     * @param usuarioAprobadorId   ID del usuario que aprueba
     * @return true si se aprobaron correctamente, false en caso contrario
     */
    public boolean aprobarAjustes(List<Integer> ajustesSeleccionados,
            Map<Integer, String> razonesSeleccionadas,
            int usuarioAprobadorId) {
        try {
            return conteoDAO.aprobarAjustes(ajustesSeleccionados, razonesSeleccionadas, usuarioAprobadorId);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null,
                    "Error al aprobar ajustes: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    /**
     * Determina el tipo de ajuste según la diferencia encontrada
     *
     * @param diferencia Diferencia entre stock contado y stock sistema
     * @param esCaja     Indica si es un ajuste de cajas o pares
     * @return Tipo de ajuste correspondiente
     */
    private String obtenerTipoAjuste(int diferencia, boolean esCaja) {
        if (diferencia > 0) {
            return esCaja ? "entrada_caja" : "entrada_par";
        } else {
            return esCaja ? "salida_caja" : "salida_par";
        }
    }

}
