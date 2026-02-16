package raven.clases.productos;

import java.util.prefs.Preferences;
import raven.application.form.other.buscador.dto.BusquedaCriteria;

/**
 * Configuración para traspasos de productos entre bodegas.
 * cinfugracion rapida
 * @autor CrisDEV
 */

public class TraspasoConfig {
    private Integer idOrigen;
    private Integer idDestino;
    private Integer idUsuarioSolicita;
    private String tipo;
    private String motivo;
    private boolean autoApply;
    private boolean mostrarIndicadorTipo;
    private boolean filtrarPorBodegaOrigen;

    public Integer getIdOrigen() { return idOrigen; }
    public void setIdOrigen(Integer idOrigen) { this.idOrigen = idOrigen; }
    public Integer getIdDestino() { return idDestino; }
    public void setIdDestino(Integer idDestino) { this.idDestino = idDestino; }
    public Integer getIdUsuarioSolicita() { return idUsuarioSolicita; }
    public void setIdUsuarioSolicita(Integer idUsuarioSolicita) { this.idUsuarioSolicita = idUsuarioSolicita; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }
    public boolean isAutoApply() { return autoApply; }
    public void setAutoApply(boolean autoApply) { this.autoApply = autoApply; }
    public boolean isMostrarIndicadorTipo() { return mostrarIndicadorTipo; }
    public void setMostrarIndicadorTipo(boolean v) { this.mostrarIndicadorTipo = v; }
    public boolean isFiltrarPorBodegaOrigen() { return filtrarPorBodegaOrigen; }
    public void setFiltrarPorBodegaOrigen(boolean v) { this.filtrarPorBodegaOrigen = v; }

    public String getTipoLabel() {
        if (tipo == null) return null;
        String t = tipo.trim().toLowerCase();
        if ("caja".equals(t)) return "Caja";
        if ("par".equals(t)) return "Par";
        return tipo;
    }

    public boolean isTipoPar() {
        return tipo != null && "par".equalsIgnoreCase(tipo.trim());
    }

    public boolean isTipoCaja() {
        return tipo != null && "caja".equalsIgnoreCase(tipo.trim());
    }

    public void aplicarEnBuscador(BusquedaCriteria.Builder builder) {
        if (idOrigen != null && idOrigen > 0) builder.enBodegaId(idOrigen);
        if (tipo != null && !tipo.isEmpty()) builder.conTipo(tipo.trim().toLowerCase());
        if (filtrarPorBodegaOrigen) builder.soloConStock(true);
        builder.soloConVariantes(true);
    }

    private static Preferences prefs() { return Preferences.userRoot().node("zapatosVersion1.0/traspasoConfig"); }

    public static TraspasoConfig load() {
        Preferences p = prefs();
        TraspasoConfig c = new TraspasoConfig();
        int o = p.getInt("idOrigen", -1);
        int d = p.getInt("idDestino", -1);
        int u = p.getInt("idUsuarioSolicita", -1);
        c.idOrigen = o > 0 ? o : null;
        c.idDestino = d > 0 ? d : null;
        c.idUsuarioSolicita = u > 0 ? u : null;
        c.tipo = p.get("tipo", null);
        c.motivo = p.get("motivo", null);
        c.autoApply = p.getBoolean("autoApply", false);
        c.mostrarIndicadorTipo = p.getBoolean("mostrarIndicadorTipo", true);
        c.filtrarPorBodegaOrigen = p.getBoolean("filtrarPorBodegaOrigen", true);
        return c;
    }

    public static void save(TraspasoConfig c) {
        Preferences p = prefs();
        p.putInt("idOrigen", c.idOrigen != null ? c.idOrigen : -1);
        p.putInt("idDestino", c.idDestino != null ? c.idDestino : -1);
        p.putInt("idUsuarioSolicita", c.idUsuarioSolicita != null ? c.idUsuarioSolicita : -1);
        if (c.tipo != null) p.put("tipo", c.tipo); else p.remove("tipo");
        if (c.motivo != null) p.put("motivo", c.motivo); else p.remove("motivo");
        p.putBoolean("autoApply", c.autoApply);
        p.putBoolean("mostrarIndicadorTipo", c.mostrarIndicadorTipo);
        p.putBoolean("filtrarPorBodegaOrigen", c.filtrarPorBodegaOrigen);
    }

    public static boolean getAutoApplyPref() {
        return prefs().getBoolean("autoApply", false);
    }
    
    public static void setAutoApplyPref(boolean v) { 
        prefs().putBoolean("autoApply", v);
    }
}
