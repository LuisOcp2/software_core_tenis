package raven.application.form.other.buscador.dto;

/**
 * Criterios de búsqueda para productos
 * Implementa el patrón Builder para facilitar la construcción
 * de filtros de búsqueda flexibles
 * 
 * Ventajas del patrón Builder:
 * - Código más legible: new BusquedaCriteria.Builder().conNombre("Adidas")...
 * - Parámetros opcionales sin constructores sobrecargados
 * - Validación centralizada
 * - Inmutable una vez construido
 * 
 * @author CrisDEV
 */

public class BusquedaCriteria {
    // Criterios de búsqueda
    private final String textoBusqueda;      // Búsqueda general en nombre, código
    private final String genero;             // Filtro por género
    private final String bodega;             // Filtro por bodega específica (ubicación)
    private final Integer idBodega;          // Filtro por id_bodega
    private final Boolean soloConStock;      // Solo productos con stock disponible
    private final Integer idCategoria;       // Filtro por categoría
    private final Integer idMarca;           // Filtro por marca
    private final Integer limite;            // Límite de resultados
    private final Integer offset;            // Offset para paginación
    private final Boolean coincidenciaExacta; // Coincidencia exacta por texto
    private final Boolean buscarPorMarca;     // Usar texto para filtrar por marca
    private final Boolean buscarPorNombre;    // Usar texto para filtrar por nombre
    private final String tipo;                // Filtro por tipo: par/caja
    private final Integer idProducto;         // Búsqueda directa por ID de producto
    private final String ean;                 // Búsqueda por código EAN
    private final Boolean soloConVariantes;   // Solo productos con variantes

    /**
     * Constructor privado - usar Builder
     */
    private BusquedaCriteria(Builder builder) {
        this.textoBusqueda = builder.textoBusqueda;
        this.genero = builder.genero;
        this.bodega = builder.bodega;
        this.idBodega = builder.idBodega;
        this.soloConStock = builder.soloConStock;
        this.idCategoria = builder.idCategoria;
        this.idMarca = builder.idMarca;
        this.limite = builder.limite;
        this.offset = builder.offset;
        this.coincidenciaExacta = builder.coincidenciaExacta;
        this.buscarPorMarca = builder.buscarPorMarca;
        this.buscarPorNombre = builder.buscarPorNombre;
        this.tipo = builder.tipo;
        this.idProducto = builder.idProducto;
        this.ean = builder.ean;
        this.soloConVariantes = builder.soloConVariantes;
    }
    
    // Getters
    
    public String getTextoBusqueda() {
        return textoBusqueda;
    }
    
    public String getGenero() {
        return genero;
    }
    
    public String getBodega() {
        return bodega;
    }
    public Integer getIdBodega() {
        return idBodega;
    }
    
    public Boolean getSoloConStock() {
        return soloConStock;
    }
    
    public Integer getIdCategoria() {
        return idCategoria;
    }
    
    public Integer getIdMarca() {
        return idMarca;
    }
    
    public Integer getLimite() {
        return limite;
    }
    
    public Integer getOffset() {
        return offset;
    }

    public Boolean getCoincidenciaExacta() {
        return coincidenciaExacta;
    }

    public Boolean getBuscarPorMarca() {
        return buscarPorMarca;
    }

    public Boolean getBuscarPorNombre() {
        return buscarPorNombre;
    }
    public String getTipo() { return tipo; }
    public Integer getIdProducto() { return idProducto; }
    public String getEan() { return ean; }
    public Boolean getSoloConVariantes() { return soloConVariantes; }
    
    /**
     * Verifica si hay algún filtro activo
     * @return true si hay al menos un criterio de búsqueda
     */
    public boolean tieneAlgunFiltro() {
        return textoBusqueda != null || genero != null || bodega != null ||
               idCategoria != null || idMarca != null || soloConStock != null;
    }
    
    /**
     * Builder para construir criterios de búsqueda de forma fluida
     */
    public static class Builder {
        private String textoBusqueda;
        private String genero;
        private String bodega;
        private Integer idBodega;
        private Boolean soloConStock;
        private Integer idCategoria;
        private Integer idMarca;
        private Integer limite = 50; // Valor por defecto
        private Integer offset = 0;  // Valor por defecto
        private Boolean coincidenciaExacta = false;
        private Boolean buscarPorMarca = true;
        private Boolean buscarPorNombre = true;
        private String tipo;
        private Integer idProducto;
        private String ean;
        private Boolean soloConVariantes = true;
        
        /**
         * Establece el texto de búsqueda general
         * Busca en nombre, código de modelo, etc.
         */
        public Builder conTextoBusqueda(String texto) {
            this.textoBusqueda = texto;
            return this;
        }
        
        /**
         * Filtra por género (HOMBRE, MUJER, NIÑO, UNISEX)
         */
        public Builder conGenero(String genero) {
            this.genero = genero;
            return this;
        }
        
        /**
         * Filtra por bodega específica
         * Si es null o "GENERAL", busca en todas las bodegas
         */
        public Builder enBodega(String bodega) {
            this.bodega = bodega;
            return this;
        }
        /**
         * Filtra por id_bodega específico. Tiene prioridad sobre "bodega" textual.
         */
        public Builder enBodegaId(Integer idBodega) {
            this.idBodega = idBodega;
            return this;
        }
        
        /**
         * Solo productos con stock disponible
         */
        public Builder soloConStock(boolean soloConStock) {
            this.soloConStock = soloConStock;
            return this;
        }
        
        /**
         * Filtra por categoría
         */
        public Builder conCategoria(Integer idCategoria) {
            this.idCategoria = idCategoria;
            return this;
        }
        
        /**
         * Filtra por marca
         */
        public Builder conMarca(Integer idMarca) {
            this.idMarca = idMarca;
            return this;
        }
        /**
         * Establece coincidencia exacta para texto de búsqueda
         */
        public Builder conCoincidenciaExacta(boolean exacta) {
            this.coincidenciaExacta = exacta;
            return this;
        }
        /**
         * Usa el texto de búsqueda para filtrar por nombre de marca
         */
        public Builder conBuscarPorMarca(boolean porMarca) {
            this.buscarPorMarca = porMarca;
            return this;
        }
        /**
         * Usa el texto de búsqueda para filtrar por nombre de producto
         */
        public Builder conBuscarPorNombre(boolean porNombre) {
            this.buscarPorNombre = porNombre;
            return this;
        }
        public Builder conTipo(String tipo) {
            this.tipo = tipo;
            return this;
        }
        public Builder conIdProducto(Integer idProducto) {
            this.idProducto = idProducto;
            return this;
        }
        public Builder conEAN(String ean) {
            this.ean = ean;
            return this;
        }
        public Builder soloConVariantes(boolean solo) {
            this.soloConVariantes = solo;
            return this;
        }
        
        /**
         * Establece el límite de resultados
         */
        public Builder conLimite(int limite) {
            if (limite > 0 && limite <= 1000) { // Validación
                this.limite = limite;
            }
            return this;
        }
        
        /**
         * Establece el offset para paginación
         */
        public Builder conOffset(int offset) {
            if (offset >= 0) {
                this.offset = offset;
            }
            return this;
        }
        
        /**
         * Construye el objeto BusquedaCriteria
         * @return Criterio de búsqueda inmutable
         */
        public BusquedaCriteria build() {
            return new BusquedaCriteria(this);
        }
    }
    
    @Override
    public String toString() {
        return String.format("BusquedaCriteria[texto=%s, genero=%s, bodega=%s, stock=%b]",
                textoBusqueda, genero, bodega, soloConStock);
    }
}
