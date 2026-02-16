package raven.controlador.productos;

import java.io.File;
import java.time.LocalDateTime;
import javax.swing.Icon;

/**
 * Modelo para las variantes de productos (combinación de talla y color)
 * Cada variante representa una versión específica de un producto base
 *
 * @author RAVEN
 * @version 2.0
 */
public class ModelProductVariant {

    // ====================================================================
    // CONSTANTES
    // ====================================================================
    private static final int PARES_POR_CAJA = 24;
    private static final String COUNTRY_PREFIX = "770";

    // ====================================================================
    // PROPIEDADES PRINCIPALES
    // ====================================================================
    private int variantId; // id_variante (PK)
    private int productId; // id_producto (FK)
    private int sizeId; // id_talla (FK)
    private int colorId; // id_color (FK)
    private int supplierId; // id_proveedor (FK)

    // ====================================================================
    // CÓDIGOS E IDENTIFICADORES
    // ====================================================================
    private String sku; // SKU único de la variante
    private String barcode; // codigo_barras único
    private String ean; // EAN único con dígito de control

    // ====================================================================
    // PRECIOS
    // ====================================================================
    private Double purchasePrice; // precio_compra específico
    private Double salePrice; // precio_venta específico

    // ====================================================================
    // STOCK
    // ====================================================================
    private Integer minStock; // stock_minimo_variante
    private Integer stockPairs;
    private Integer stockBoxes;
    private Integer inventoryId; // Added missing field

    // ====================================================================
    // DATOS RELACIONADOS (JOINs)
    // ====================================================================
    private String sizeName;
    private String colorName;
    private String sizeSystem;
    private String colorHex;
    private String gender;

    // ====================================================================
    // UBICACIÓN
    // ====================================================================
    private String warehouseLocation; // ubicacion_bodega
    private String storeLocation; // ubicacion_tienda
    private Integer warehouseId; // id_bodega

    // ====================================================================
    // OTROS
    // ====================================================================
    private Integer weightGrams; // peso_gramos
    private String observations; // observaciones
    private java.sql.Date expirationDate; // fecha_vencimiento
    private boolean available; // disponible
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ====================================================================
    // CAMPOS LEGADO (Mantenidos por compatibilidad temporal)
    // ====================================================================
    private int id; // alias for variantId
    private String talla; // alias for sizeName
    private String color; // alias for colorName
    private String proveedor; // alias for supplierName (if added)
    private int stock; // alias for stockPairs?
    private String unidad; 
    private String bodega; // alias for warehouse name
    private Icon icon;
    private File path;
    private int bodegaId; // alias for warehouseId
    
    // Campos para manejo de imágenes
    private byte[] imageBytes;
    private boolean hasImage;

    public ModelProductVariant() {
    }

    // Constructor completo (ajustar según necesidad)
    public ModelProductVariant(int id, int productId, String talla, String color, String proveedor, int stock, String unidad, String bodega, Icon icon, File path, int bodegaId) {
        this.id = id;
        this.variantId = id;
        this.productId = productId;
        this.talla = talla;
        this.sizeName = talla;
        this.color = color;
        this.colorName = color;
        this.proveedor = proveedor;
        this.stock = stock;
        this.stockPairs = stock;
        this.unidad = unidad;
        this.bodega = bodega;
        this.icon = icon;
        this.path = path;
        this.bodegaId = bodegaId;
        this.warehouseId = bodegaId;
    }

    public ModelProductVariant(int productId, int sizeId, int colorId, int stock) {
        this.productId = productId;
        this.sizeId = sizeId;
        this.colorId = colorId;
        this.stockPairs = stock;
        this.stock = stock;
    }

    public void generateSku(String modelCode) {
        if (this.sku == null || this.sku.trim().isEmpty()) {
            StringBuilder sb = new StringBuilder();
            if (modelCode != null && !modelCode.isEmpty()) {
                sb.append(modelCode).append("-");
            }
            sb.append(sizeId).append("-").append(colorId);
            this.sku = sb.toString().toUpperCase();
        }
    }

    public void generateEanIfEmpty() {
        if (this.ean == null || this.ean.trim().isEmpty()) {
            // Generar EAN temporal basado en tiempo si está vacío
            // Formato simple: Prefijo país + timestamp
            long time = System.currentTimeMillis();
            this.ean = COUNTRY_PREFIX + String.valueOf(time).substring(3);
        }
    }

    // Getters y Setters
    
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
        this.variantId = id;
    }

    public int getProductId() {
        return productId;
    }

    public void setProductId(int productId) {
        this.productId = productId;
    }

    public String getTalla() {
        return talla;
    }

    public void setTalla(String talla) {
        this.talla = talla;
        this.sizeName = talla;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
        this.colorName = color;
    }

    public String getProveedor() {
        return proveedor;
    }

    public void setProveedor(String proveedor) {
        this.proveedor = proveedor;
    }

    public int getStock() {
        return stock;
    }

    public void setStock(int stock) {
        this.stock = stock;
        this.stockPairs = stock;
    }

    public String getUnidad() {
        return unidad;
    }

    public void setUnidad(String unidad) {
        this.unidad = unidad;
    }

    public String getBodega() {
        return bodega;
    }

    public void setBodega(String bodega) {
        this.bodega = bodega;
    }

    public Icon getIcon() {
        return icon;
    }

    public void setIcon(Icon icon) {
        this.icon = icon;
    }

    public File getPath() {
        return path;
    }

    public void setPath(File path) {
        this.path = path;
    }

    public int getBodegaId() {
        return bodegaId;
    }

    public void setBodegaId(int bodegaId) {
        this.bodegaId = bodegaId;
        this.warehouseId = bodegaId;
    }

    public int getVariantId() {
        return variantId;
    }

    public void setVariantId(int variantId) {
        this.variantId = variantId;
        this.id = variantId;
    }

    public int getSizeId() {
        return sizeId;
    }

    public void setSizeId(int sizeId) {
        this.sizeId = sizeId;
    }

    public int getColorId() {
        return colorId;
    }

    public void setColorId(int colorId) {
        this.colorId = colorId;
    }

    public int getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(int supplierId) {
        this.supplierId = supplierId;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    public String getEan() {
        return ean;
    }

    public void setEan(String ean) {
        this.ean = ean;
    }

    public Double getPurchasePrice() {
        return purchasePrice;
    }

    public void setPurchasePrice(Double purchasePrice) {
        this.purchasePrice = purchasePrice;
    }

    public Double getSalePrice() {
        return salePrice;
    }

    public void setSalePrice(Double salePrice) {
        this.salePrice = salePrice;
    }

    public Integer getMinStock() {
        return minStock;
    }

    public void setMinStock(Integer minStock) {
        this.minStock = minStock;
    }

    public Integer getInventoryId() {
        return inventoryId;
    }

    public void setInventoryId(Integer inventoryId) {
        this.inventoryId = inventoryId;
    }

    public String getWarehouseLocation() {
        return warehouseLocation;
    }

    public void setWarehouseLocation(String warehouseLocation) {
        this.warehouseLocation = warehouseLocation;
    }

    public String getStoreLocation() {
        return storeLocation;
    }

    public void setStoreLocation(String storeLocation) {
        this.storeLocation = storeLocation;
    }

    public Integer getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Integer warehouseId) {
        this.warehouseId = warehouseId;
        this.bodegaId = warehouseId;
    }

    public Integer getWeightGrams() {
        return weightGrams;
    }

    public void setWeightGrams(Integer weightGrams) {
        this.weightGrams = weightGrams;
    }

    public String getObservations() {
        return observations;
    }

    public void setObservations(String observations) {
        this.observations = observations;
    }

    public java.sql.Date getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(java.sql.Date expirationDate) {
        this.expirationDate = expirationDate;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getSizeName() {
        return sizeName;
    }

    public void setSizeName(String sizeName) {
        this.sizeName = sizeName;
        this.talla = sizeName;
    }

    public String getColorName() {
        return colorName;
    }

    public void setColorName(String colorName) {
        this.colorName = colorName;
        this.color = colorName;
    }

    public String getSizeSystem() {
        return sizeSystem;
    }

    public void setSizeSystem(String sizeSystem) {
        this.sizeSystem = sizeSystem;
    }

    public String getColorHex() {
        return colorHex;
    }

    public void setColorHex(String colorHex) {
        this.colorHex = colorHex;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public byte[] getImageBytes() {
        return imageBytes;
    }

    public void setImageBytes(byte[] imageBytes) {
        this.imageBytes = imageBytes;
        this.hasImage = (imageBytes != null && imageBytes.length > 0);
    }

    public boolean hasImage() {
        return hasImage;
    }

    public void setHasImage(boolean hasImage) {
        this.hasImage = hasImage;
    }

    public int getStockPairs() {
        return stockPairs != null ? stockPairs : 0;
    }

    public void setStockPairs(int stockPairs) {
        this.stockPairs = stockPairs;
        this.stock = stockPairs;
    }

    public int getStockBoxes() {
        return stockBoxes != null ? stockBoxes : 0;
    }

    public void setStockBoxes(int stockBoxes) {
        this.stockBoxes = stockBoxes;
    }

    @Override
    public String toString() {
        return String.format(
                "ModelProductVariant{id=%d, sku='%s', barcode='%s', ean='%s', size='%s', color='%s', stockPairs=%s, stockBoxes=%s, hasImage=%s}",
                variantId, sku, barcode, ean, sizeName, colorName,
                stockPairs != null ? stockPairs : 0,
                stockBoxes != null ? stockBoxes : 0,
                hasImage);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;

        ModelProductVariant that = (ModelProductVariant) obj;
        return variantId == that.variantId;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(variantId);
    }
}
