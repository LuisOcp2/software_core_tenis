package raven.application.form.productos.buscador;

public class ProductoBusquedaItem {
    private int idVariante;
    private String ean;
    private String nombre;
    private String talla;
    private String genero;
    private int stock;
    private String tipo; // "Pares" o "Cajas"
    private String bodega;
    private String marca;
    private String color;
    private int idProducto;

    public ProductoBusquedaItem() {
    }

    public ProductoBusquedaItem(int idVariante, String ean, String nombre, String talla, String genero, int stock, String tipo, String bodega, String marca, String color, int idProducto) {
        this.idVariante = idVariante;
        this.ean = ean;
        this.nombre = nombre;
        this.talla = talla;
        this.genero = genero;
        this.stock = stock;
        this.tipo = tipo;
        this.bodega = bodega;
        this.marca = marca;
        this.color = color;
        this.idProducto = idProducto;
    }

    public int getIdVariante() { return idVariante; }
    public void setIdVariante(int idVariante) { this.idVariante = idVariante; }

    public String getEan() { return ean; }
    public void setEan(String ean) { this.ean = ean; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getTalla() { return talla; }
    public void setTalla(String talla) { this.talla = talla; }

    public String getGenero() { return genero; }
    public void setGenero(String genero) { this.genero = genero; }

    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = stock; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public String getBodega() { return bodega; }
    public void setBodega(String bodega) { this.bodega = bodega; }

    public String getMarca() { return marca; }
    public void setMarca(String marca) { this.marca = marca; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public int getIdProducto() { return idProducto; }
    public void setIdProducto(int idProducto) { this.idProducto = idProducto; }
}
