package raven.componentes.impresion;

import java.math.BigDecimal;
import java.sql.Blob;
import java.sql.Timestamp;

public class DataSearch {

    private String id_prod;
    private String EAN;
    private String SKU;
    private String nombre;
    private String color;
    private String talla;
    private boolean startsWithSearch;
    // Nuevos campos
    private String descripcion;
    private int idCategoria;
    private int idMarca;
    private int idProveedor;
    private BigDecimal precioCompra;
    private BigDecimal precioVenta;
    private int stock;
    private int stockMinimo;
    private String genero;
    private boolean activo;
    private Blob imagen;
    private Timestamp fechaCreacion;
    private int stockPorCajas;
    private int stockPorPares;
    private int paresPorCaja;
    private int idVariante; // ID de la variante del producto

    public DataSearch() {
    }

    public DataSearch(String id_prod, String EAN, String SKU, String nombre, String color, String talla, boolean startsWithSearch, String descripcion, int idCategoria, int idMarca, int idProveedor, BigDecimal precioCompra, BigDecimal precioVenta, int stock, int stockMinimo, String genero, boolean activo, Blob imagen, Timestamp fechaCreacion, int stockPorCajas, int stockPorPares, int paresPorCaja, int idVariante) {
        this.id_prod = id_prod;
        this.EAN = EAN;
        this.SKU = SKU;
        this.nombre = nombre;
        this.color = color;
        this.talla = talla;
        this.startsWithSearch = startsWithSearch;
        this.descripcion = descripcion;
        this.idCategoria = idCategoria;
        this.idMarca = idMarca;
        this.idProveedor = idProveedor;
        this.precioCompra = precioCompra;
        this.precioVenta = precioVenta;
        this.stock = stock;
        this.stockMinimo = stockMinimo;
        this.genero = genero;
        this.activo = activo;
        this.imagen = imagen;
        this.fechaCreacion = fechaCreacion;
        this.stockPorCajas = stockPorCajas;
        this.stockPorPares = stockPorPares;
        this.paresPorCaja = paresPorCaja;
        this.idVariante = idVariante;
    }

    public DataSearch(String id_prod, String EAN, String SKU, String nombre, String color, String talla,
            boolean startsWithSearch, String descripcion, int idCategoria, int idMarca,
            int idProveedor, BigDecimal precioCompra, BigDecimal precioVenta, int stock,
            int stockMinimo, String genero, boolean activo, Timestamp fechaCreacion,
            int stockPorCajas, int stockPorPares, int paresPorCaja) {
        this.id_prod = id_prod;
        this.EAN = EAN;
        this.SKU = SKU;
        this.nombre = nombre;
        this.color = color;
        this.talla = talla;
        this.startsWithSearch = startsWithSearch;
        this.descripcion = descripcion;
        this.idCategoria = idCategoria;
        this.idMarca = idMarca;
        this.idProveedor = idProveedor;
        this.precioCompra = precioCompra;
        this.precioVenta = precioVenta;
        this.stock = stock;
        this.stockMinimo = stockMinimo;
        this.genero = genero;
        this.activo = activo;
        this.fechaCreacion = fechaCreacion;
        this.stockPorCajas = stockPorCajas;
        this.stockPorPares = stockPorPares;
        this.paresPorCaja = paresPorCaja;
        // idVariante y imagen quedan con valores por defecto (0 y null)
    }

    public DataSearch(String id_prod, String EAN, String nombre, String color, String talla, boolean startsWithSearch) {
        this.id_prod = id_prod;
        this.EAN = EAN;
        this.nombre = nombre;
        this.color = color;
        this.talla = talla;
        this.startsWithSearch = startsWithSearch;
    }

    public DataSearch(String id_prod, String SKU, String nombre, String color, String talla,
            boolean startsWithSearch, String descripcion, int idCategoria, int idMarca,
            int idProveedor, BigDecimal precioCompra, BigDecimal precioVenta, int stock,
            int stockMinimo, String genero, boolean activo, Blob imagen,
            Timestamp fechaCreacion, int stockPorCajas, int stockPorPares,
            int paresPorCaja, int idVariante) {
        this.id_prod = id_prod;
        this.SKU = SKU;
        this.nombre = nombre;
        this.color = color;
        this.talla = talla;
        this.startsWithSearch = startsWithSearch;
        this.descripcion = descripcion;
        this.idCategoria = idCategoria;
        this.idMarca = idMarca;
        this.idProveedor = idProveedor;
        this.precioCompra = precioCompra;
        this.precioVenta = precioVenta;
        this.stock = stock;
        this.stockMinimo = stockMinimo;
        this.genero = genero;
        this.activo = activo;
        this.imagen = imagen;
        this.fechaCreacion = fechaCreacion;
        this.stockPorCajas = stockPorCajas;
        this.stockPorPares = stockPorPares;
        this.paresPorCaja = paresPorCaja;
        this.idVariante = idVariante;
    }

    public DataSearch(
            String idprod,
            String EAN,
            String SKU,
            String nombre,
            String color,
            String talla,
            boolean startsWithSearch,
            String descripcion,
            int idCategoria,
            int idMarca,
            int idProveedor,
            BigDecimal precioCompra,
            BigDecimal precioVenta,
            int stock,
            int stockMinimo,
            String genero,
            boolean activo,
            Blob imagen,
            Timestamp fechaCreacion,
            int stockPorCajas,
            int stockPorPares,
            int paresPorCaja
    ) {
        this.id_prod = idprod;
        this.EAN = EAN;
        this.SKU = SKU;
        this.nombre = nombre;
        this.color = color;
        this.talla = talla;
        this.startsWithSearch = startsWithSearch;
        this.descripcion = descripcion;
        this.idCategoria = idCategoria;
        this.idMarca = idMarca;
        this.idProveedor = idProveedor;
        this.precioCompra = precioCompra;
        this.precioVenta = precioVenta;
        this.stock = stock;
        this.stockMinimo = stockMinimo;
        this.genero = genero;
        this.activo = activo;
        this.imagen = imagen;
        this.fechaCreacion = fechaCreacion;
        this.stockPorCajas = stockPorCajas;
        this.stockPorPares = stockPorPares;
        this.paresPorCaja = paresPorCaja;
        this.idVariante = 0; // Por defecto
    }
    public DataSearch(
        String idprod,
        String EAN,
        String SKU,
        String nombre,
        String color,
        String talla,
        boolean startsWithSearch,
        String descripcion,
        int idCategoria,
        int idMarca,
        int idProveedor,
        BigDecimal precioCompra,
        BigDecimal precioVenta,
        int stockPorPares,      // WARNING  ATENCIÓN: Va ANTES de stockMinimo
        int stockMinimo,        // WARNING  ATENCIÓN: Va DESPUÉS de stockPorPares
        String genero,
        boolean activo,
        Blob imagen,            //  BLOB va ANTES de Timestamp
        Timestamp fechaCreacion, //  Timestamp va DESPUÉS de Blob
        int stockPorCajas,
        int paresPorCaja
) {
    // Inicialización de campos básicos
    this.id_prod = idprod;
    this.EAN = EAN;
    this.SKU = SKU;
    this.nombre = nombre;
    this.color = color;
    this.talla = talla;
    this.startsWithSearch = startsWithSearch;
    
    // Campos de negocio
    this.descripcion = descripcion;
    this.idCategoria = idCategoria;
    this.idMarca = idMarca;
    this.idProveedor = idProveedor;
    
    // Campos monetarios
    this.precioCompra = precioCompra;
    this.precioVenta = precioVenta;
    
    // Campos de inventario (WARNING  NOTA: el orden importa)
    this.stockPorPares = stockPorPares;  // Este es el parámetro que llamas "stockPares"
    this.stock = stockPorPares;          // Sincronizamos stock general con pares
    this.stockMinimo = stockMinimo;
    this.stockPorCajas = stockPorCajas;
    this.paresPorCaja = paresPorCaja;
    
    // Campos descriptivos
    this.genero = genero;
    this.activo = activo;
    
    // Campos multimedia y temporales
    this.imagen = imagen;
    this.fechaCreacion = fechaCreacion;
    
    // Campo de variante con valor por defecto
    this.idVariante = 0;
}

  

    public String getId_prod() {
        return id_prod;
    }

    public void setId_prod(String id_prod) {
        this.id_prod = id_prod;
    }

    public String getEAN() {
        return EAN;
    }

    public void setEAN(String EAN) {
        this.EAN = EAN;
    }

    public String getSKU() {
        return SKU;
    }

    public void setSKU(String SKU) {
        this.SKU = SKU;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getTalla() {
        return talla;
    }

    public void setTalla(String talla) {
        this.talla = talla;
    }

    public boolean isStartsWithSearch() {
        return startsWithSearch;
    }

    public void setStartsWithSearch(boolean startsWithSearch) {
        this.startsWithSearch = startsWithSearch;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public int getIdCategoria() {
        return idCategoria;
    }

    public void setIdCategoria(int idCategoria) {
        this.idCategoria = idCategoria;
    }

    public int getIdMarca() {
        return idMarca;
    }

    public void setIdMarca(int idMarca) {
        this.idMarca = idMarca;
    }

    public int getIdProveedor() {
        return idProveedor;
    }

    public void setIdProveedor(int idProveedor) {
        this.idProveedor = idProveedor;
    }

    public BigDecimal getPrecioCompra() {
        return precioCompra;
    }

    public void setPrecioCompra(BigDecimal precioCompra) {
        this.precioCompra = precioCompra;
    }

    public BigDecimal getPrecioVenta() {
        return precioVenta;
    }

    public void setPrecioVenta(BigDecimal precioVenta) {
        this.precioVenta = precioVenta;
    }

    public int getStock() {
        return stock;
    }

    public void setStock(int stock) {
        this.stock = stock;
    }

    public int getStockMinimo() {
        return stockMinimo;
    }

    public void setStockMinimo(int stockMinimo) {
        this.stockMinimo = stockMinimo;
    }

    public String getGenero() {
        return genero;
    }

    public void setGenero(String genero) {
        this.genero = genero;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    public Blob getImagen() {
        return imagen;
    }

    public void setImagen(Blob imagen) {
        this.imagen = imagen;
    }

    public Timestamp getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(Timestamp fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public int getStockPorCajas() {
        return stockPorCajas;
    }

    public void setStockPorCajas(int stockPorCajas) {
        this.stockPorCajas = stockPorCajas;
    }

    public int getStockPorPares() {
        return stockPorPares;
    }

    public void setStockPorPares(int stockPorPares) {
        this.stockPorPares = stockPorPares;
    }

    public int getParesPorCaja() {
        return paresPorCaja;
    }

    public void setParesPorCaja(int paresPorCaja) {
        this.paresPorCaja = paresPorCaja;
    }

    public int getIdVariante() {
        return idVariante;
    }

    public void setIdVariante(int idVariante) {
        this.idVariante = idVariante;
    }

    @Override
    public String toString() {
        return id_prod + " - " + nombre + " - " + color + " - " + talla;
    }
}

