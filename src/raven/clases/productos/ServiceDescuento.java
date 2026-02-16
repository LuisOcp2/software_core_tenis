package raven.clases.productos;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import raven.controlador.productos.ModelPromocion;
import raven.dao.DescuentoDAO;

/**
 * Servicio para manejar las operaciones de promociones/descuentos
 * Utiliza DescuentoDAO para las operaciones de base de datos
 * @author CrisDEV
 */
public class ServiceDescuento {
    
    private final DescuentoDAO descuentoDAO;
    
    /**
     * Constructor que inicializa el DAO
     */
    public ServiceDescuento() {
        this.descuentoDAO = new DescuentoDAO();
    }
    
    /**
     * Obtiene todas las promociones de la base de datos
     * @return Lista de promociones
     * @throws SQLException Si ocurre un error en la base de datos
     */
    public List<ModelPromocion> getAll() throws SQLException {
        try {
            List<ModelPromocion> promociones = descuentoDAO.obtenerTodasLasPromociones();
            System.out.println("SUCCESS  Cargadas " + promociones.size() + " promociones desde la base de datos");
            return promociones;
        } catch (SQLException e) {
            System.err.println("ERROR  Error cargando promociones: " + e.getMessage());
            throw e;
        }
    }
    
    /**
     * Obtiene solo las promociones activas
     * @return Lista de promociones activas
     * @throws SQLException Si ocurre un error en la base de datos
     */
    public List<ModelPromocion> getActive() throws SQLException {
        try {
            List<ModelPromocion> todasLasPromociones = descuentoDAO.obtenerTodasLasPromociones();
            List<ModelPromocion> promocionesActivas = new ArrayList<>();
            
            // Filtrar solo las promociones activas y vigentes
            for (ModelPromocion promocion : todasLasPromociones) {
                if (promocion.isActiva() && 
                    promocion.getFechaInicio() != null && 
                    promocion.getFechaFin() != null) {
                    
                    LocalDateTime ahora = LocalDateTime.now();
                    if (!promocion.getFechaInicio().isAfter(ahora) && 
                        !promocion.getFechaFin().isBefore(ahora)) {
                        promocionesActivas.add(promocion);
                    }
                }
            }
            
            System.out.println("SUCCESS  Cargadas " + promocionesActivas.size() + " promociones activas");
            return promocionesActivas;
        } catch (SQLException e) {
            System.err.println("ERROR  Error cargando promociones activas: " + e.getMessage());
            throw e;
        }
    }
    
    /**
     * Obtiene una promoción por su ID
     * @param idPromocion ID de la promoción
     * @return Promoción encontrada o null si no existe
     * @throws SQLException Si ocurre un error en la base de datos
     */
    public ModelPromocion getById(int idPromocion) throws SQLException {
        try {
            ModelPromocion promocion = descuentoDAO.obtenerPromocionPorId(idPromocion);
            if (promocion != null) {
                System.out.println("SUCCESS  Promoción encontrada: " + promocion.getNombre());
            } else {
                System.out.println("WARNING  No se encontró promoción con ID: " + idPromocion);
            }
            return promocion;
        } catch (SQLException e) {
            System.err.println("ERROR  Error obteniendo promoción por ID " + idPromocion + ": " + e.getMessage());
            throw e;
        }
    }
    
    /**
     * Busca promociones por término de búsqueda
     * @param searchTerm Término de búsqueda
     * @return Lista de promociones que coinciden con la búsqueda
     * @throws SQLException Si ocurre un error en la base de datos
     */
    public List<ModelPromocion> search(String searchTerm) throws SQLException {
        try {
            List<ModelPromocion> promociones = descuentoDAO.buscarPromociones(searchTerm);
            System.out.println("SUCCESS  Encontradas " + promociones.size() + " promociones para: " + searchTerm);
            return promociones;
        } catch (SQLException e) {
            System.err.println("ERROR  Error buscando promociones: " + e.getMessage());
            throw e;
        }
    }
    
    /**
     * Inserta una nueva promoción
     * @param promocion Promoción a insertar
     * @return true si se insertó correctamente, false en caso contrario
     * @throws SQLException Si ocurre un error en la base de datos
     */
    public int insertarPromocion(ModelPromocion promocion) throws SQLException {
        //se necesita arreglar bien esto
//        try {
//            int resultado = descuentoDAO.insertarPromocion(promocion);
//            if (!resultado) {
//                System.out.println("ERROR  No se pudo insertar la promoción: " + promocion.getNombre());
//            } else {
//                System.out.println("SUCCESS  Promoción insertada correctamente: " + promocion.getNombre());
//            }
//            return resultado;
//        } catch (SQLException e) {
//            System.err.println("ERROR  Error insertando promoción: " + e.getMessage());
//            throw e;
//        }
return 0;// opcional
    }
    
    /**
     * Actualiza una promoción existente
     * @param promocion Promoción a actualizar
     * @return true si se actualizó correctamente, false en caso contrario
     * @throws SQLException Si ocurre un error en la base de datos
     */
    public boolean actualizarPromocion(ModelPromocion promocion) throws SQLException {
        try {
            boolean resultado = descuentoDAO.actualizarPromocion(promocion);
            if (resultado) {
                System.out.println("SUCCESS  Promoción actualizada correctamente: " + promocion.getNombre());
            } else {
                System.out.println("ERROR  No se pudo actualizar la promoción: " + promocion.getNombre());
            }
            return resultado;
        } catch (SQLException e) {
            System.err.println("Error actualizando promoción: " + e.getMessage());
            throw e;
        }
    }
    
    /**
     * Elimina (desactiva) una promoción
     * @param idPromocion ID de la promoción a eliminar
     * @return true si se eliminó correctamente, false en caso contrario
     * @throws SQLException Si ocurre un error en la base de datos
     */
    public boolean eliminarPromocion(int idPromocion) throws SQLException {
        try {
            boolean resultado = descuentoDAO.eliminarPromocion(idPromocion);
            if (resultado) {
                System.out.println("Promoción eliminada correctamente con ID: " + idPromocion);
            } else {
                System.out.println("No se pudo eliminar la promoción con ID: " + idPromocion);
            }
            return resultado;
        } catch (SQLException e) {
            System.err.println("Error eliminando promoción: " + e.getMessage());
            throw e;
        }
    }
    
    /**
     * Verifica si existe una promoción con un nombre específico
     * @param nombre Nombre de la promoción
     * @return true si existe, false si no
     */
    public boolean existePromocion(String nombre) {
        try {
            List<ModelPromocion> promociones = descuentoDAO.buscarPromociones(nombre);
            for (ModelPromocion promocion : promociones) {
                if (promocion.getNombre().equalsIgnoreCase(nombre.trim())) {
                    return true;
                }
            }
            return false;
        } catch (SQLException e) {
            System.err.println("Error verificando existencia de promoción: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Verifica si existe una promoción con un código específico
     * @param codigo Código de la promoción
     * @return true si existe, false si no
     */
    public boolean existeCodigoPromocion(String codigo) {
        try {
            List<ModelPromocion> promociones = descuentoDAO.buscarPromociones(codigo);
            for (ModelPromocion promocion : promociones) {
                if (promocion.getCodigo() != null && 
                    promocion.getCodigo().equalsIgnoreCase(codigo.trim())) {
                    return true;
                }
            }
            return false;
        } catch (SQLException e) {
            System.err.println("Error verificando existencia de código de promoción: " + e.getMessage());
            return false;
        }
    }
}

