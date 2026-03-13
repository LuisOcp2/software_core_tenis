package raven.controlador.productos;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import raven.controlador.comercial.ModelSupplier;

public class ModelProduct {

    private int productId;
    private int variantId; // NUEVO - ID de la variante para conteos
    private String barcode;
    private String name;
    private String description;
    private ModelCategory category;
    private ModelBrand brand;
    private ModelSupplier supplier;
    private double purchasePrice;   // Precio base
    private double salePrice;       // Precio base

    public void setUbicacion(String ubicacion) {
        this.ubicacion = ubicacion;
    }

    public String getUbicacion() {
        return ubicacion;
    }
     private String ubicacion;
    private int minStock;
    private String size;
    private String color;
    private String gender;
    private boolean active;
    private int boxesStock;
    private int pairsStock;
    private int pairsPerBox;
    private ModelProfile profile = new ModelProfile();
    private byte[] imageBytes;
    
    private String modelCode;  // NUEVO - codigo_modelo
    private String closureType;     // NUEVO - tipo_cierre
    private String style;           // NUEVO - estilo
    
    // NUEVO - Lista de variantes
    private List<ModelProductVariant> variants = new ArrayList<>();

    public ModelProduct(int productId, String barcode, String name, String description, 
                   ModelCategory category, ModelBrand brand, ModelSupplier supplier, 
                   double purchasePrice, double salePrice, int minStock, String size, 
                   String color, String gender, boolean active, int boxesStock, 
                   int pairsStock, int pairsPerBox, ModelProfile profile, String ubicacion) {
        this.productId = productId;
        this.barcode = barcode;
        this.name = name;
        this.description = description;
        this.category = category;
        this.brand = brand;
        this.supplier = supplier;
        this.purchasePrice = purchasePrice;
        this.salePrice = salePrice;
        this.minStock = minStock;
        this.size = size;
        this.color = color;
        this.gender = gender;
        this.active = active;
        this.boxesStock = boxesStock;
        this.pairsStock = pairsStock;
        this.pairsPerBox = pairsPerBox;
        this.profile = profile;
        this.ubicacion = ubicacion;
}
    /*public ModelProduct(int productId, String barcode, String name, String description, ModelCategory category, ModelBrand brand, ModelSupplier supplier, double purchasePrice, double salePrice, int minStock, String size, String color, String gender, boolean active, int boxesStock, int pairsStock, int pairsPerBox, ModelProfile profile) {
        this.productId = productId;
        this.barcode = barcode;
        this.name = name;
        this.description = description;
        this.category = category;
        this.brand = brand;
        this.supplier = supplier;
        this.purchasePrice = purchasePrice;
        this.salePrice = salePrice;
        this.minStock = minStock;
        this.size = size;
        this.color = color;
        this.gender = gender;
        this.active = active;
        this.boxesStock = boxesStock;
        this.pairsStock = pairsStock;
        this.pairsPerBox = pairsPerBox;
        this.profile = profile;
    }*/

    public ModelProduct() {
    }

    public ModelProduct(String nombre, String codigoBarras, String color, String size) {
        this.name = name;
        this.barcode = barcode;
        this.color = color;
        this.size = size;
    }

    
    private transient javax.swing.Icon cachedIcon;

    public javax.swing.Icon getCachedIcon() {
        return cachedIcon;
    }

    public void setCachedIcon(javax.swing.Icon cachedIcon) {
        this.cachedIcon = cachedIcon;
    }

    public ModelProfile getProfile() {
        if (profile == null) {
            profile = new ModelProfile(); // Doble verificación
        }
        return profile;
    }
    
    private transient List<ImagenVarianteTemp> imagenesTemp;
    
    // Getter y setter  
    public List<ImagenVarianteTemp> getImagenesTemp() {
        return imagenesTemp;
    }
    
    public void setImagenesTemp(List<ImagenVarianteTemp> imagenesTemp) {
        this.imagenesTemp = imagenesTemp;
    }
    
    

    // Getters y Setters existentes + nuevos
    
    public String getModelCode() { 
        return modelCode; 
    }
    
    public void setModelCode(String modelCode) { 
        this.modelCode = modelCode; 
    }
    
    public String getClosureType() { 
        return closureType; 
    }
    public void setClosureType(String closureType) { 
        this.closureType = closureType; 
    }
    
    public String getStyle() { 
        return style; 
    }
    public void setStyle(String style) { 
        this.style = style;
    }
    
    public List<ModelProductVariant> getVariants() { 
        return variants; 
    }
    
    
    public void setVariants(List<ModelProductVariant> variants) {
        this.variants = variants; 
    }
    
    public void addVariant(ModelProductVariant variant) {
        this.variants.add(variant);
    }
    
    public void setProfile(ModelProfile profile) {
        this.profile = profile;
    }

    public int getProductId() {
        return productId;
    }

    public void setProductId(int productId) {
        this.productId = productId;
    }

    public int getVariantId() {
        return variantId;
    }

    public void setVariantId(int variantId) {
        this.variantId = variantId;
    }

    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ModelCategory getCategory() {
        return category;
    }

    public void setCategory(ModelCategory category) {
        this.category = category;
    }

    public ModelBrand getBrand() {
        return brand;
    }

    public void setBrand(ModelBrand brand) {
        this.brand = brand;
    }

    public ModelSupplier getSupplier() {
        return supplier;
    }

    public void setSupplier(ModelSupplier supplier) {
        this.supplier = supplier;
    }

    public double getPurchasePrice() {
        return purchasePrice;
    }

    public void setPurchasePrice(double purchasePrice) {
        this.purchasePrice = purchasePrice;
    }

    public double getSalePrice() {
        return salePrice;
    }

    public void setSalePrice(double salePrice) {
        this.salePrice = salePrice;
    }

    public int getMinStock() {
        return minStock;
    }

    public void setMinStock(int minStock) {
        this.minStock = minStock;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public int getBoxesStock() {
        return boxesStock;
    }

    public void setBoxesStock(int boxesStock) {
        this.boxesStock = boxesStock;
    }   

    public int getPairsStock() {
        return pairsStock;
    }

    public void setPairsStock(int pairsStock) {
        this.pairsStock = pairsStock;
    }
    
    // Métodos calculados
    public int getTotalPairsStock() {
        return variants.stream().mapToInt(ModelProductVariant::getStockPairs).sum();
    }
    
    public int getTotalBoxesStock() {
        return variants.stream().mapToInt(ModelProductVariant::getStockBoxes).sum();
    }

    public int getPairsPerBox() {
        return pairsPerBox;
    }

    // Getters y setters (manteniendo los existentes y añadiendo)
    public void setPairsPerBox(int pairsPerBox) {
        this.pairsPerBox = pairsPerBox;
    }

    public byte[] getImageBytes() {
        return imageBytes;
    }

    public void setImageBytes(byte[] imageBytes) {
        this.imageBytes = imageBytes;
    }

    public Object[] toTableRow3(int rowNum) {

        return new Object[]{
            false,
            rowNum,
            this,
            productId,
            barcode,
            brand,
            color,
            size,
            pairsStock,
            boxesStock,
            gender,};

    }
    public Object[] toTableRow(int rowNumber) {
        int totalPares = getVariants().stream().mapToInt(ModelProductVariant::getStockPairs).sum();
        int totalCajas = getVariants().stream().mapToInt(ModelProductVariant::getStockBoxes).sum();

        String tallas = getVariants().stream()
            .map(ModelProductVariant::getSizeName) // Asegúrate que este método exista
            .distinct()
            .sorted()
            .collect(Collectors.joining(", "));

        String colores = getVariants().stream()
            .map(ModelProductVariant::getColorName) // Asegúrate que este método exista
            .distinct()
            .sorted()
            .collect(Collectors.joining(", "));

        return new Object[] {
            false, // Checkbox
            rowNumber,
            this, // Para render personalizado
            getProductId(),
            getBrand().getName(),
            colores,
            tallas,
            totalPares,
            totalCajas
        };
}

    

    public Object[] toTableRowRot(int rowNum) {

        return new Object[]{
           //"SELECT", "EAN", "NOMBRE", "COLOR", "TALLA", "CANTIDAD", "Id"
            false,
            barcode,
            name,
            color,
            size,
            1,
            productId
        };

    }

    @Override
    public String toString() {
        return "ModelProduct{" + "productId=" + productId + ", barcode=" + barcode + ", name=" + name + ", description=" + description + ", category=" + category + ", brand=" + brand + ", supplier=" + supplier + ", purchasePrice=" + purchasePrice + ", salePrice=" + salePrice + ", minStock=" + minStock + ", size=" + size + ", color=" + color + ", gender=" + gender + ", active=" + active + ", boxesStock=" + boxesStock + ", pairsStock=" + pairsStock + ", pairsPerBox=" + pairsPerBox + ", profile=" + profile + '}';
    }
    

}
