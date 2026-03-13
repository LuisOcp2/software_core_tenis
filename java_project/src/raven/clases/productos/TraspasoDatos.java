/*
 * TraspasoDatos - VERSIÓN FINAL CORREGIDA
 * Sin errores de compilación
 */
package raven.clases.productos;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class TraspasoDatos {

    private String numeroTraspaso;
    private LocalDateTime fechaSolicitud;
    private Integer idBodegaOrigen;
    private String nombreBodegaOrigen;
    private Integer idBodegaDestino;
    private String nombreBodegaDestino;
    private String tipoTraspaso;
    private String motivoTraspaso;
    private String observaciones;
    private Integer idUsuarioSolicita;
    private Integer idUsuarioCrea;
    private List<ProductoTraspasoItem> productos;
    private String estado;
    private boolean confirmado = false;
    private java.math.BigDecimal montoTotal = java.math.BigDecimal.ZERO;
    private java.math.BigDecimal montoRecibido = java.math.BigDecimal.ZERO;

    public boolean isConfirmado() {
        return confirmado;
    }

    public void setConfirmado(boolean confirmado) {
        this.confirmado = confirmado;
    }

    public TraspasoDatos() {
        this.productos = new ArrayList<>();
        this.fechaSolicitud = LocalDateTime.now();
        this.estado = "pendiente";
        this.confirmado = false;
    }

    // Getters y Setters básicos
    public String getNumeroTraspaso() {
        return numeroTraspaso;
    }

    public void setNumeroTraspaso(String numeroTraspaso) {
        this.numeroTraspaso = numeroTraspaso;
    }

    public LocalDateTime getFechaSolicitud() {
        return fechaSolicitud;
    }

    public void setFechaSolicitud(LocalDateTime fechaSolicitud) {
        this.fechaSolicitud = fechaSolicitud;
    }

    public Integer getIdBodegaOrigen() {
        return idBodegaOrigen;
    }

    public void setIdBodegaOrigen(Integer idBodegaOrigen) {
        this.idBodegaOrigen = idBodegaOrigen;
    }

    public String getNombreBodegaOrigen() {
        return nombreBodegaOrigen;
    }

    public void setNombreBodegaOrigen(String nombreBodegaOrigen) {
        this.nombreBodegaOrigen = nombreBodegaOrigen;
    }

    public Integer getIdBodegaDestino() {
        return idBodegaDestino;
    }

    public void setIdBodegaDestino(Integer idBodegaDestino) {
        this.idBodegaDestino = idBodegaDestino;
    }

    public String getNombreBodegaDestino() {
        return nombreBodegaDestino;
    }

    public void setNombreBodegaDestino(String nombreBodegaDestino) {
        this.nombreBodegaDestino = nombreBodegaDestino;
    }

    public String getTipoTraspaso() {
        return tipoTraspaso;
    }

    public void setTipoTraspaso(String tipoTraspaso) {
        this.tipoTraspaso = tipoTraspaso;
    }

    public String getMotivoTraspaso() {
        return motivoTraspaso;
    }

    public void setMotivoTraspaso(String motivoTraspaso) {
        this.motivoTraspaso = motivoTraspaso;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }

    public Integer getIdUsuarioSolicita() {
        return idUsuarioSolicita;
    }

    public void setIdUsuarioSolicita(Integer idUsuarioSolicita) {
        this.idUsuarioSolicita = idUsuarioSolicita;
    }

    public Integer getIdUsuarioCrea() {
        return idUsuarioCrea;
    }

    public void setIdUsuarioCrea(Integer idUsuarioCrea) {
        this.idUsuarioCrea = idUsuarioCrea;
    }

    public List<ProductoTraspasoItem> getProductos() {
        return productos;
    }

    public void setProductos(List<ProductoTraspasoItem> productos) {
        this.productos = productos != null ? productos : new ArrayList<>();
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    // Métodos de manipulación de productos
    public void agregarProducto(ProductoTraspasoItem producto) {
        if (this.productos == null) {
            this.productos = new ArrayList<>();
        }
        this.productos.add(producto);
        calcularTotales();
    }

    public void removerProducto(int index) {
        if (productos != null && index >= 0 && index < productos.size()) {
            productos.remove(index);
            calcularTotales();
        }
    }

    public void removerProducto(ProductoTraspasoItem producto) {
        if (productos != null) {
            productos.remove(producto);
            calcularTotales();
        }
    }

    public ProductoTraspasoItem obtenerProducto(int index) {
        if (productos != null && index >= 0 && index < productos.size()) {
            return productos.get(index);
        }
        return null;
    }

    // Métodos de utilidad mejorados
    public double getTotalProductos() {
        return productos != null ? productos.size() : 0;
    }

    public int getCantidadTotalItems() {
        if (productos == null) {
            return 0;
        }

        int total = 0;
        for (ProductoTraspasoItem producto : productos) {
            if (producto.getCantidadSolicitada() != null) {
                total += producto.getCantidadSolicitada();
            }
        }
        return total;
    }

    public int getCantidadTotalPares() {
        if (productos == null) {
            return 0;
        }

        int totalPares = 0;
        for (ProductoTraspasoItem producto : productos) {
            if (producto.getCantidadSolicitada() != null) {
                if ("par".equalsIgnoreCase(producto.getTipo())) {
                    totalPares += producto.getCantidadSolicitada();
                } else if ("caja".equalsIgnoreCase(producto.getTipo())) {
                    // Asumir 24 pares por caja si no está especificado
                    int paresPorCaja = producto.getParesPorCaja() != null ? producto.getParesPorCaja() : 24;
                    totalPares += producto.getCantidadSolicitada() * paresPorCaja;
                }
            }
        }
        return totalPares;
    }

    public int getCantidadTotalCajas() {
        if (productos == null) {
            return 0;
        }

        int totalCajas = 0;
        for (ProductoTraspasoItem producto : productos) {
            if (producto.getCantidadSolicitada() != null && "caja".equalsIgnoreCase(producto.getTipo())) {
                totalCajas += producto.getCantidadSolicitada();
            }
        }
        return totalCajas;
    }

    public java.math.BigDecimal getMontoTotal() {
        return montoTotal;
    }

    public void setMontoTotal(java.math.BigDecimal montoTotal) {
        this.montoTotal = montoTotal;
    }

    public java.math.BigDecimal getMontoRecibido() {
        return montoRecibido;
    }

    public void setMontoRecibido(java.math.BigDecimal montoRecibido) {
        this.montoRecibido = montoRecibido;
    }

    public void calcularTotales() {
        java.math.BigDecimal total = java.math.BigDecimal.ZERO;
        if (productos != null) {
            for (ProductoTraspasoItem p : productos) {
                total = total.add(p.calcularSubtotal());
            }
        }
        this.montoTotal = total;
    }

    /**
     * Validación mejorada que verifica todos los campos requeridos
     */
    public boolean isValid() {
        // Verificar datos básicos
        boolean datosBasicos = numeroTraspaso != null && !numeroTraspaso.trim().isEmpty()
                && idBodegaOrigen != null && idBodegaDestino != null
                && !idBodegaOrigen.equals(idBodegaDestino)
                && tipoTraspaso != null && !tipoTraspaso.trim().isEmpty()
                && !"Seleccionar".equals(tipoTraspaso)
                && !"Seleccionar".equals(tipoTraspaso);
        // && motivoTraspaso != null && motivoTraspaso.trim().length() >= 10; // Motivo
        // opcional

        // Verificar que hay productos
        boolean tieneProductos = productos != null && !productos.isEmpty();

        // Verificar que todos los productos son válidos
        boolean productosValidos = true;
        if (productos != null) {
            for (ProductoTraspasoItem producto : productos) {
                if (!producto.esValido()) {
                    productosValidos = false;
                    break;
                }
            }
        }

        return datosBasicos && tieneProductos && productosValidos;
    }

    /**
     * Verifica si solo faltan productos para ser válido
     */
    public boolean validoDatosSinProductos() {
        return numeroTraspaso != null && !numeroTraspaso.trim().isEmpty()
                && idBodegaOrigen != null && idBodegaDestino != null
                && !idBodegaOrigen.equals(idBodegaDestino)
                && tipoTraspaso != null && !tipoTraspaso.trim().isEmpty()
                && !"Seleccionar".equals(tipoTraspaso)
                && !"Seleccionar".equals(tipoTraspaso);
        // && motivoTraspaso != null && motivoTraspaso.trim().length() >= 10; // Motivo
        // opcional
    }

    /**
     * Obtiene un resumen rápido del traspaso
     */
    public String getResumenRapido() {
        StringBuilder resumen = new StringBuilder();
        resumen.append("Traspaso: ").append(numeroTraspaso != null ? numeroTraspaso : "Sin número");
        resumen.append(" | ");
        resumen.append(nombreBodegaOrigen != null ? nombreBodegaOrigen : "Sin origen");
        resumen.append(" → ");
        resumen.append(nombreBodegaDestino != null ? nombreBodegaDestino : "Sin destino");
        resumen.append(" | ");
        resumen.append(productos != null ? productos.size() : 0).append(" productos");
        return resumen.toString();
    }

    /**
     * Encuentra un producto por ID y variante
     */
    public ProductoTraspasoItem buscarProducto(Integer idProducto, Integer idVariante, String tipo) {
        if (productos == null) {
            return null;
        }

        for (ProductoTraspasoItem producto : productos) {
            if (producto.getIdProducto().equals(idProducto)
                    && Objects.equals(producto.getIdVariante(), idVariante)
                    && producto.getTipo().equals(tipo)) {
                return producto;
            }
        }
        return null;
    }

    /**
     * Verifica si un producto ya está en la lista
     */
    public boolean contieneProducto(Integer idProducto, Integer idVariante, String tipo) {
        return buscarProducto(idProducto, idVariante, tipo) != null;
    }

    /**
     * Limpia todos los productos
     */
    public void limpiarProductos() {
        if (productos != null) {
            productos.clear();
        }
    }

    /**
     * Reinicia todos los datos del traspaso
     */
    public void reiniciar() {
        this.numeroTraspaso = null;
        this.fechaSolicitud = LocalDateTime.now();
        this.idBodegaOrigen = null;
        this.nombreBodegaOrigen = null;
        this.idBodegaDestino = null;
        this.nombreBodegaDestino = null;
        this.tipoTraspaso = null;
        this.motivoTraspaso = null;
        this.observaciones = null;
        this.idUsuarioSolicita = null;
        this.idUsuarioCrea = null;
        this.estado = "pendiente";
        this.confirmado = false;

        if (productos != null) {
            productos.clear();
        }
    }

    /**
     * Crea una copia del traspaso (sin productos)
     */
    public TraspasoDatos clonarSinProductos() {
        TraspasoDatos copia = new TraspasoDatos();
        copia.setIdBodegaOrigen(this.idBodegaOrigen);
        copia.setNombreBodegaOrigen(this.nombreBodegaOrigen);
        copia.setIdBodegaDestino(this.idBodegaDestino);
        copia.setNombreBodegaDestino(this.nombreBodegaDestino);
        copia.setTipoTraspaso(this.tipoTraspaso);
        copia.setMotivoTraspaso(this.motivoTraspaso);
        copia.setObservaciones(this.observaciones);
        copia.setIdUsuarioSolicita(this.idUsuarioSolicita);
        copia.setIdUsuarioCrea(this.idUsuarioCrea);
        return copia;
    }

    /**
     * MÉTODO CORREGIDO - Verifica si los datos han cambiado respecto a otro
     * traspaso
     */
    public boolean haCambiado(TraspasoDatos otro) {
        if (otro == null) {
            return true;
        }

        // Manejar productos null de manera segura
        int thisProductosSize = (this.productos != null) ? this.productos.size() : 0;
        int otroProductosSize = (otro.productos != null) ? otro.productos.size() : 0;

        return !Objects.equals(this.numeroTraspaso, otro.numeroTraspaso)
                || !Objects.equals(this.idBodegaOrigen, otro.idBodegaOrigen)
                || !Objects.equals(this.idBodegaDestino, otro.idBodegaDestino)
                || !Objects.equals(this.tipoTraspaso, otro.tipoTraspaso)
                || !Objects.equals(this.motivoTraspaso, otro.motivoTraspaso)
                || !Objects.equals(this.observaciones, otro.observaciones)
                || thisProductosSize != otroProductosSize;
    }

    /**
     * Obtiene información detallada para debugging
     */
    public String getInfoDebug() {
        StringBuilder info = new StringBuilder();
        info.append("TraspasoDatos {\n");
        info.append("  numeroTraspaso: ").append(numeroTraspaso).append("\n");
        info.append("  fechaSolicitud: ").append(fechaSolicitud).append("\n");
        info.append("  idBodegaOrigen: ").append(idBodegaOrigen).append("\n");
        info.append("  nombreBodegaOrigen: ").append(nombreBodegaOrigen).append("\n");
        info.append("  idBodegaDestino: ").append(idBodegaDestino).append("\n");
        info.append("  nombreBodegaDestino: ").append(nombreBodegaDestino).append("\n");
        info.append("  tipoTraspaso: ").append(tipoTraspaso).append("\n");
        info.append("  motivoTraspaso: ").append(motivoTraspaso != null ? motivoTraspaso.length() + " chars" : "null")
                .append("\n");
        info.append("  estado: ").append(estado).append("\n");
        info.append("  confirmado: ").append(confirmado).append("\n");
        info.append("  productos: ").append(productos != null ? productos.size() : 0).append(" items\n");
        info.append("  isValid: ").append(isValid()).append("\n");
        info.append("}");
        return info.toString();
    }

    /**
     * Valida un campo específico
     */
    public boolean validarCampo(String campo) {
        switch (campo.toLowerCase()) {
            case "numero":
                return numeroTraspaso != null && !numeroTraspaso.trim().isEmpty();
            case "bodegaorigen":
                return idBodegaOrigen != null;
            case "bodegadestino":
                return idBodegaDestino != null;
            case "bodegas":
                return idBodegaOrigen != null && idBodegaDestino != null && !idBodegaOrigen.equals(idBodegaDestino);
            case "tipo":
                return tipoTraspaso != null && !tipoTraspaso.trim().isEmpty() && !"Seleccionar".equals(tipoTraspaso);
            case "motivo":
                return true; // motivoTraspaso != null && motivoTraspaso.trim().length() >= 10; // Opcional
            case "productos":
                return productos != null && !productos.isEmpty();
            default:
                return false;
        }
    }

    /**
     * Obtiene una lista de campos faltantes
     */
    public List<String> getCamposFaltantes() {
        List<String> faltantes = new ArrayList<>();

        if (!validarCampo("numero")) {
            faltantes.add("Número de traspaso");
        }
        if (!validarCampo("bodegaorigen")) {
            faltantes.add("Bodega de origen");
        }
        if (!validarCampo("bodegadestino")) {
            faltantes.add("Bodega de destino");
        } else if (idBodegaOrigen != null && idBodegaOrigen.equals(idBodegaDestino)) {
            faltantes.add("Bodegas diferentes (origen ≠ destino)");
        }
        if (!validarCampo("tipo")) {
            faltantes.add("Tipo de traspaso");
        }
        // if (!validarCampo("motivo")) {
        // faltantes.add("Motivo (mínimo 10 caracteres)");
        // }
        if (!validarCampo("productos")) {
            faltantes.add("Al menos un producto");
        }

        return faltantes;
    }

    @Override
    public String toString() {
        return getResumenRapido();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        TraspasoDatos that = (TraspasoDatos) obj;
        return Objects.equals(numeroTraspaso, that.numeroTraspaso);
    }

    @Override
    public int hashCode() {
        return numeroTraspaso != null ? numeroTraspaso.hashCode() : 0;
    }
}
