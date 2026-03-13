package raven.clases.productos;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import java.io.*;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

/**
 * Gestor de perfiles de impresión para etiquetas
 * Permite guardar, cargar y gestionar configuraciones de impresión
 */
public class GestorPerfilesImpresion {
    private static final String PREFS_NODE = "raven.etiquetas.perfiles";
    private static final String PERFILES_KEY = "perfiles_json";
    private static final String VERSION_KEY = "version";
    private static final int VERSION_ACTUAL = 1;

    private static GestorPerfilesImpresion instancia;
    private final Preferences prefs;
    private final ObjectMapper objectMapper;
    private List<PerfilImpresion> perfiles;

    private GestorPerfilesImpresion() {
        this.prefs = Preferences.userRoot().node(PREFS_NODE);
        this.objectMapper = new ObjectMapper();
        this.perfiles = cargarPerfiles();
    }
    
    public static synchronized GestorPerfilesImpresion getInstancia() {
        if (instancia == null) {
            instancia = new GestorPerfilesImpresion();
        }
        return instancia;
    }
    
    /**
     * Carga los perfiles desde preferencias
     */
    private List<PerfilImpresion> cargarPerfiles() {
        int versionGuardada = prefs.getInt(VERSION_KEY, 0);

        if (versionGuardada != VERSION_ACTUAL) {
            // Si la versión no coincide, devolver lista vacía o crear perfiles por defecto
            return crearPerfilesPorDefecto();
        }

        String json = prefs.get(PERFILES_KEY, null);
        if (json == null || json.trim().isEmpty()) {
            return crearPerfilesPorDefecto();
        }

        try {
            List<PerfilImpresion> lista = objectMapper.readValue(json, new TypeReference<List<PerfilImpresion>>() {});
            return lista != null ? lista : crearPerfilesPorDefecto();
        } catch (Exception e) {
            System.err.println("Error al cargar perfiles: " + e.getMessage());
            return crearPerfilesPorDefecto();
        }
    }
    
    /**
     * Crea perfiles por defecto
     */
    private List<PerfilImpresion> crearPerfilesPorDefecto() {
        List<PerfilImpresion> lista = new ArrayList<>();
        
        // Perfil para etiquetas pequeñas (40x20mm)
        PerfilImpresion pequena = new PerfilImpresion(
            "Pequeña (40x20mm)",
            40.0, 20.0,           // Ancho x Alto
            2.0,                  // Margen
            "LANDSCAPE",          // Orientación
            true,                 // AutoFit
            1.0,                  // Escala
            0,                    // Rotación
            0.0, 0.0,            // Offsets
            "Par",                // Tipo
            false                 // XP420B
        );
        lista.add(pequena);
        
        // Perfil para etiquetas medianas (50x30mm)
        PerfilImpresion mediana = new PerfilImpresion(
            "Mediana (50x30mm)",
            50.0, 30.0,           // Ancho x Alto
            2.5,                  // Margen
            "LANDSCAPE",          // Orientación
            true,                 // AutoFit
            1.0,                  // Escala
            0,                    // Rotación
            0.0, 0.0,            // Offsets
            "Par",                // Tipo
            false                 // XP420B
        );
        lista.add(mediana);
        
        // Perfil para etiquetas grandes (105x25mm)
        PerfilImpresion grande = new PerfilImpresion(
            "Grande (105x25mm)",
            105.0, 25.0,         // Ancho x Alto
            3.0,                 // Margen
            "LANDSCAPE",         // Orientación
            true,                // AutoFit
            1.0,                 // Escala
            0,                   // Rotación
            0.0, 0.0,           // Offsets
            "Caja",              // Tipo
            false                // XP420B
        );
        lista.add(grande);
        
        return lista;
    }
    
    /**
     * Guarda los perfiles en preferencias
     */
    private void guardarPerfiles() {
        try {
            String json = objectMapper.writeValueAsString(perfiles);
            prefs.put(PERFILES_KEY, json);
            prefs.putInt(VERSION_KEY, VERSION_ACTUAL);
            prefs.flush(); // Asegurar que se guarden inmediatamente
        } catch (Exception e) {
            System.err.println("Error al guardar perfiles: " + e.getMessage());
        }
    }
    
    /**
     * Obtiene todos los perfiles
     */
    public List<PerfilImpresion> getPerfiles() {
        return new ArrayList<>(perfiles);
    }
    
    /**
     * Añade un nuevo perfil
     */
    public void agregarPerfil(PerfilImpresion perfil) {
        if (perfil == null) return;
        
        // Verificar que no exista un perfil con el mismo nombre
        for (PerfilImpresion p : perfiles) {
            if (p.getNombre().equals(perfil.getNombre())) {
                throw new IllegalArgumentException("Ya existe un perfil con este nombre: " + perfil.getNombre());
            }
        }
        
        perfiles.add(perfil);
        guardarPerfiles();
    }
    
    /**
     * Actualiza un perfil existente
     */
    public void actualizarPerfil(PerfilImpresion perfil) {
        if (perfil == null) return;
        
        for (int i = 0; i < perfiles.size(); i++) {
            if (perfiles.get(i).getNombre().equals(perfil.getNombre())) {
                perfiles.set(i, perfil);
                guardarPerfiles();
                return;
            }
        }
        throw new IllegalArgumentException("Perfil no encontrado: " + perfil.getNombre());
    }
    
    /**
     * Elimina un perfil
     */
    public void eliminarPerfil(String nombre) {
        perfiles.removeIf(p -> p.getNombre().equals(nombre));
        guardarPerfiles();
    }
    
    /**
     * Obtiene un perfil por nombre
     */
    public PerfilImpresion obtenerPerfil(String nombre) {
        for (PerfilImpresion p : perfiles) {
            if (p.getNombre().equals(nombre)) {
                return p.copiar(); // Devolver copia para evitar modificaciones accidentales
            }
        }
        return null;
    }
    
    /**
     * Obtiene el perfil por defecto
     */
    public PerfilImpresion obtenerPerfilPorDefecto() {
        if (perfiles.isEmpty()) {
            return new PerfilImpresion();
        }
        return perfiles.get(0).copiar();
    }
    
    /**
     * Verifica si existe un perfil con el nombre dado
     */
    public boolean existePerfil(String nombre) {
        for (PerfilImpresion p : perfiles) {
            if (p.getNombre().equals(nombre)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Reinicia los perfiles a los valores por defecto
     */
    public void reiniciarPerfiles() {
        perfiles = crearPerfilesPorDefecto();
        guardarPerfiles();
    }
}