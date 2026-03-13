package raven.dao;

import java.sql.*;
import java.math.BigDecimal;
import raven.controlador.productos.ModelProduct;
import raven.controlador.productos.ModelBrand;
import raven.controlador.productos.ModelCategory;
import raven.controlador.comercial.ModelSupplier;
import raven.controlador.principal.conexion;

/**
 * DAO optimizado para la gestión de productos.
 * 
 * MEJORAS IMPLEMENTADAS:
 * - Campos completos en INSERT/UPDATE según estructura real de BD
 * - Manejo defensivo de valores NULL
 * - Auditoría automática con fecha_actualizacion
 * - Validaciones de integridad antes de persistir
 * - Mensajes de error descriptivos
 * 
 * PRINCIPIOS APLICADOS:
 * - Single Responsibility: Solo persistencia de productos
 * - Fail-Fast: Validaciones tempranas
 * - Defensive Programming: Manejo robusto de NULL
 * 
 * @author CrisDEV
 * @version 2.1 - Ajustado sin ubicacion_bodega/ubicacion_tienda
 */
public class ProductosDAO {
    
    private static String toDbGenero(String genero) {
        if (genero == null) return null;
        String g = genero.trim().toUpperCase();
        if ("NIÑO".equals(g)) return "NIÃO";
        return g;
    }
    /**
     * Obtiene un producto por su ID con todos sus campos.
     * 
     * @param id ID del producto
     * @return ModelProduct o null si no existe
     * @throws SQLException Error de base de datos
     */
    public ModelProduct getById(int id) throws SQLException {
        String sql = "SELECT " +
                    "id_producto, codigo_modelo, nombre, descripcion, " +
                    "id_categoria, id_marca, id_proveedor, " +
                    "precio_compra, precio_venta, stock_minimo, " +
                    "talla, color, genero, activo, " +
                    "ubicacion, pares_por_caja " +
                    "FROM productos WHERE id_producto=?";
        
        try (Connection con = conexion.getInstance().createConnection(); 
             PreparedStatement pst = con.prepareStatement(sql)) {
            
            pst.setInt(1, id);
            
            try (ResultSet rs = pst.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                
                ModelProduct p = new ModelProduct();
                
                // IDs y códigos
                p.setProductId(rs.getInt("id_producto"));
                p.setModelCode(rs.getString("codigo_modelo"));
                
                // Información básica
                p.setName(rs.getString("nombre"));
                p.setDescription(rs.getString("descripcion"));
                
                // Precios - Convertir BigDecimal a double
                BigDecimal precioCompra = rs.getBigDecimal("precio_compra");
                BigDecimal precioVenta = rs.getBigDecimal("precio_venta");
                p.setPurchasePrice(precioCompra != null ? precioCompra.doubleValue() : 0.0);
                p.setSalePrice(precioVenta != null ? precioVenta.doubleValue() : 0.0);
                
                // Stock y configuración
                p.setMinStock(rs.getInt("stock_minimo"));
                p.setGender(rs.getString("genero"));
                p.setUbicacion(rs.getString("ubicacion"));
                p.setPairsPerBox(rs.getInt("pares_por_caja"));
                p.setActive(rs.getBoolean("activo"));
                
                return p;
            }
        }
    }
    
    /**
     * Inserta un nuevo producto con todos los campos necesarios.
     * 
     * PATRÓN: Defensive Programming
     * - Valida datos obligatorios antes de insertar
     * - Maneja valores NULL apropiadamente
     * - Retorna ID generado o lanza excepción si falla
     * 
     * @param product Producto a insertar
     * @return ID del producto creado
     * @throws SQLException Error de base de datos
     * @throws IllegalArgumentException Si datos son inválidos
     */
    public int insert(ModelProduct product) throws SQLException {
        // SUCCESS  VALIDACIÓN DE ENTRADA - Fail Fast Pattern
        if (product == null) {
            throw new IllegalArgumentException("El producto no puede ser null");
        }
        
        if (product.getName() == null || product.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre del producto es obligatorio");
        }
        
        // SUCCESS  SQL SIN ubicacion_bodega ni ubicacion_tienda
        String sql = "INSERT INTO productos (" +
                    "codigo_modelo, nombre, descripcion, " +
                    "id_categoria, id_marca, id_proveedor, " +
                    "precio_compra, precio_venta, stock_minimo, " +
                    "talla, color, genero, " +
                    "activo, ubicacion, pares_por_caja" +
                    ") VALUES (" +
                    "?, ?, ?, " +  // codigo_modelo, nombre, descripcion
                    "?, ?, ?, " +  // id_categoria, id_marca, id_proveedor
                    "?, ?, ?, " +  // precio_compra, precio_venta, stock_minimo
                    "?, ?, ?, " +  // talla, color, genero
                    "TRUE, ?, ?" + // activo=TRUE, ubicacion, pares_por_caja
                    ")";
        
        try (Connection con = conexion.getInstance().createConnection(); 
             PreparedStatement pst = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            int paramIndex = 1;
            
            // 1. Código modelo
            pst.setString(paramIndex++, product.getModelCode());
            
            // 2-3. Información básica
            pst.setString(paramIndex++, product.getName());
            pst.setString(paramIndex++, product.getDescription());
            
            // 4-6. Relaciones (IDs) - Permitir NULL con validación
            if (product.getCategory() != null && product.getCategory().getCategoryId() > 0) {
                pst.setInt(paramIndex++, product.getCategory().getCategoryId());
            } else {
                pst.setNull(paramIndex++, java.sql.Types.INTEGER);
            }
            
            if (product.getBrand() != null && product.getBrand().getBrandId() > 0) {
                pst.setInt(paramIndex++, product.getBrand().getBrandId());
            } else {
                pst.setNull(paramIndex++, java.sql.Types.INTEGER);
            }
            
            if (product.getSupplier() != null && product.getSupplier().getSupplierId() > 0) {
                pst.setInt(paramIndex++, product.getSupplier().getSupplierId());
            } else {
                pst.setNull(paramIndex++, java.sql.Types.INTEGER);
            }
            
            // 7-8. Precios - Usar BigDecimal con validación
            double precioCompra = product.getPurchasePrice() >= 0 ? product.getPurchasePrice() : 0.0;
            double precioVenta = product.getSalePrice() >= 0 ? product.getSalePrice() : 0.0;
            pst.setBigDecimal(paramIndex++, BigDecimal.valueOf(precioCompra));
            pst.setBigDecimal(paramIndex++, BigDecimal.valueOf(precioVenta));
            
            // 9. Stock mínimo
            int stockMinimo = product.getMinStock() >= 0 ? product.getMinStock() : 1;
            pst.setInt(paramIndex++, stockMinimo);
            
            // 10-12. Talla, Color, Género - pueden ser NULL
            pst.setString(paramIndex++, product.getSize());
            pst.setString(paramIndex++, product.getColor());
            pst.setString(paramIndex++, toDbGenero(product.getGender()));
            
            // 13-14. Ubicación general y pares por caja
            pst.setString(paramIndex++, product.getUbicacion());
            int paresPorCaja = product.getPairsPerBox() > 0 ? product.getPairsPerBox() : 24;
            pst.setInt(paramIndex++, paresPorCaja);
            
            // Ejecutar inserción
            int affectedRows = pst.executeUpdate();
            
            if (affectedRows == 0) {
                throw new SQLException("La inserción del producto falló, no se afectaron filas");
            }
            
            // Obtener ID generado
            try (ResultSet rs = pst.getGeneratedKeys()) {
                if (rs.next()) {
                    int generatedId = rs.getInt(1);
                    System.out.println("SUCCESS  Producto insertado con ID: " + generatedId);
                    return generatedId;
                } else {
                    throw new SQLException("La inserción del producto falló, no se obtuvo ID");
                }
            }
        }
    }
    
    /**
     * Actualiza un producto existente con todos los campos necesarios.
     * 
     * MEJORAS:
     * - Actualiza fecha_actualizacion automáticamente
     * - Validación estricta de entrada
     * - Verifica que el producto exista antes de actualizar
     * 
     * @param p Producto con datos actualizados
     * @throws SQLException Error de base de datos
     * @throws IllegalArgumentException Si datos son inválidos
     */
    public void update(ModelProduct p) throws SQLException {
        // SUCCESS  VALIDACIÓN DE ENTRADA
        if (p == null) {
            throw new IllegalArgumentException("El producto no puede ser null");
        }
        
        if (p.getProductId() <= 0) {
            throw new IllegalArgumentException("ID de producto inválido: " + p.getProductId());
        }
        
        if (p.getName() == null || p.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre del producto es obligatorio");
        }
        
        // SUCCESS  SQL SIN ubicacion_bodega ni ubicacion_tienda
        String sql = "UPDATE productos SET " +
                    "codigo_modelo=?, " +
                    "nombre=?, " +
                    "descripcion=?, " +
                    "id_categoria=?, " +
                    "id_marca=?, " +
                    "id_proveedor=?, " +
                    "precio_compra=?, " +
                    "precio_venta=?, " +
                    "stock_minimo=?, " +
                    "talla=?, " +
                    "color=?, " +
                    "genero=?, " +
                    "activo=?, " +
                    "ubicacion=?, " +
                    "pares_por_caja=?, " +
                    "fecha_actualizacion=CURRENT_TIMESTAMP " + // SUCCESS  AUDITORÍA
                    "WHERE id_producto=?";
        
        try (Connection con = conexion.getInstance().createConnection(); 
             PreparedStatement pst = con.prepareStatement(sql)) {
            
            int paramIndex = 1;
            
            // 1. Código modelo
            pst.setString(paramIndex++, p.getModelCode());
            
            // 2-3. Información básica
            pst.setString(paramIndex++, p.getName());
            pst.setString(paramIndex++, p.getDescription());
            
            // 4-6. Relaciones (permitir NULL)
            if (p.getCategory() != null && p.getCategory().getCategoryId() > 0) {
                pst.setInt(paramIndex++, p.getCategory().getCategoryId());
            } else {
                pst.setNull(paramIndex++, java.sql.Types.INTEGER);
            }
            
            if (p.getBrand() != null && p.getBrand().getBrandId() > 0) {
                pst.setInt(paramIndex++, p.getBrand().getBrandId());
            } else {
                pst.setNull(paramIndex++, java.sql.Types.INTEGER);
            }
            
            if (p.getSupplier() != null && p.getSupplier().getSupplierId() > 0) {
                pst.setInt(paramIndex++, p.getSupplier().getSupplierId());
            } else {
                pst.setNull(paramIndex++, java.sql.Types.INTEGER);
            }
            
            // 7-8. Precios
            double precioCompra = p.getPurchasePrice() >= 0 ? p.getPurchasePrice() : 0.0;
            double precioVenta = p.getSalePrice() >= 0 ? p.getSalePrice() : 0.0;
            pst.setBigDecimal(paramIndex++, BigDecimal.valueOf(precioCompra));
            pst.setBigDecimal(paramIndex++, BigDecimal.valueOf(precioVenta));
            
            // 9. Stock mínimo
            int stockMinimo = p.getMinStock() >= 0 ? p.getMinStock() : 1;
            pst.setInt(paramIndex++, stockMinimo);
            
            // 10-12. Talla, Color, Género
            pst.setString(paramIndex++, p.getSize());
            pst.setString(paramIndex++, p.getColor());
            pst.setString(paramIndex++, toDbGenero(p.getGender()));
            
            // 13. Activo
            pst.setBoolean(paramIndex++, p.isActive());
            
            // 14-15. Ubicación y pares por caja
            pst.setString(paramIndex++, p.getUbicacion());
            int paresPorCaja = p.getPairsPerBox() > 0 ? p.getPairsPerBox() : 24;
            pst.setInt(paramIndex++, paresPorCaja);
            
            // 16. WHERE - ID del producto
            pst.setInt(paramIndex++, p.getProductId());
            
            // Ejecutar actualización
            int affectedRows = pst.executeUpdate();
            
            if (affectedRows == 0) {
                throw new SQLException(
                    "La actualización falló, producto no encontrado con ID: " + p.getProductId()
                );
            }
            
            System.out.println("SUCCESS  Producto actualizado: ID=" + p.getProductId() + 
                             ", Nombre=" + p.getName());
        }
    }

    public int getMaxProductId() throws SQLException {
        String sql = "SELECT COALESCE(MAX(id_producto),0) FROM productos";
        try (Connection con = conexion.getInstance().createConnection(); PreparedStatement pst = con.prepareStatement(sql); ResultSet rs = pst.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        }
        return 0;
    }

    public ModelProduct getFullById(int id) throws SQLException {
        String sql = "SELECT p.id_producto,p.codigo_modelo,p.nombre,p.descripcion,p.id_categoria,p.id_marca,p.id_proveedor,p.precio_compra,p.precio_venta,p.stock_minimo,p.talla,p.color,p.genero,p.activo,p.ubicacion,p.pares_por_caja, " +
                     "c.id_categoria AS cat_id,c.nombre AS cat_nombre, m.id_marca AS marca_id,m.nombre AS marca_nombre, pr.id_proveedor AS prov_id, pr.nombre AS prov_nombre " +
                     "FROM productos p " +
                     "LEFT JOIN categorias c ON p.id_categoria=c.id_categoria " +
                     "LEFT JOIN marcas m ON p.id_marca=m.id_marca " +
                     "LEFT JOIN proveedores pr ON p.id_proveedor=pr.id_proveedor " +
                     "WHERE p.id_producto=?";
        try (Connection con = conexion.getInstance().createConnection(); PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setInt(1, id);
            try (ResultSet rs = pst.executeQuery()) {
                if (!rs.next()) return null;
                ModelProduct p = new ModelProduct();
                p.setProductId(rs.getInt("id_producto"));
                p.setModelCode(rs.getString("codigo_modelo"));
                p.setName(rs.getString("nombre"));
                p.setDescription(rs.getString("descripcion"));
                p.setPurchasePrice(rs.getBigDecimal("precio_compra") != null ? rs.getBigDecimal("precio_compra").doubleValue() : 0);
                p.setSalePrice(rs.getBigDecimal("precio_venta") != null ? rs.getBigDecimal("precio_venta").doubleValue() : 0);
                p.setMinStock(rs.getInt("stock_minimo"));
                p.setSize(rs.getString("talla"));
                p.setColor(rs.getString("color"));
                p.setGender(rs.getString("genero"));
                p.setActive(rs.getBoolean("activo"));
                p.setUbicacion(rs.getString("ubicacion"));
                p.setPairsPerBox(rs.getInt("pares_por_caja"));
                // Relaciones
                ModelCategory cat = new ModelCategory();
                cat.setCategoryId(rs.getInt("cat_id"));
                cat.setName(rs.getString("cat_nombre"));
                p.setCategory(cat);
                ModelBrand mb = new ModelBrand();
                mb.setBrandId(rs.getInt("marca_id"));
                mb.setName(rs.getString("marca_nombre"));
                p.setBrand(mb);
                ModelSupplier sp = new ModelSupplier();
                sp.setSupplierId(rs.getInt("prov_id"));
                sp.setName(rs.getString("prov_nombre"));
                p.setSupplier(sp);
                return p;
            }
        }
    }
}

