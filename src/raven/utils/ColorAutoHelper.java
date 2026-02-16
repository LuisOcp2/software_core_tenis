package raven.utils;

import java.text.Normalizer;
import java.util.*;

/**
 *
 * @author CrisDEV
 */
public class ColorAutoHelper {
    
    static class Info {
        String hex, pantone;
        Info(String h, String p) {
            hex = h;
            pantone = p;
        }
    }
    
    /**
     * Remueve acentos y normaliza el texto
     * @param s Texto a normalizar
     * @return Texto sin acentos y en minúsculas
     */
    private static String unaccent(String s) {
        s = s.toLowerCase(Locale.ROOT);
        s = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", ""); // quita acentos
        return s.replace('ñ', 'n');
    }
    
    /**
     * Parsea el nombre del color para extraer colores individuales
     * @param nombre Nombre del color a parsear
     * @return Lista de colores individuales
     */
    private static List<String> parseNombre(String nombre) {
        String s = " " + unaccent(nombre) + " ";
        s = s.replace(" y ", "/").replace(",", "/").replace("+", "/");
        s = s.replaceAll("\\s*/\\s*", "/").trim();
        if (s.startsWith("/")) s = s.substring(1);
        if (s.endsWith("/")) s = s.substring(0, s.length() - 1);
        List<String> out = new ArrayList<>();
        for (String t : s.split("/")) {
            t = t.trim();
            if (!t.isEmpty()) out.add(t);
            if (out.size() == 5) break;
        }
        return out;
    }
    
    /**
     * Genera automáticamente los códigos hex y pantone basándose en el nombre del color
     * @param nombre Nombre del color
     * @return Mapa con codigo_hex y codigo_pantone
     */
    public static Map<String, String> autoColorCodes(String nombre) {
        Map<String, Info> catalog = new HashMap<>();
        catalog.put("blanco", new Info("#FFFFFF", "White"));
        catalog.put("negro", new Info("#000000", "Black C"));
        catalog.put("gris", new Info("#808080", "Cool Gray 7 C"));
        catalog.put("rojo", new Info("#FF0000", "186 C"));
        catalog.put("azul", new Info("#0000FF", "2935 C"));
        catalog.put("verde", new Info("#008000", "348 C"));
        catalog.put("amarillo", new Info("#FFFF00", "Process Yellow C"));
        catalog.put("beige", new Info("#F5F5DC", "468 C"));
        catalog.put("rosa", new Info("#FFC0CB", "1895 C"));
        catalog.put("marron", new Info("#8B4513", "469 C"));
        catalog.put("dorado", new Info("#FFD700", "871 C"));
        catalog.put("plateado", new Info("#C0C0C0", "877 C"));
        catalog.put("purpura", new Info("#800080", "2685 C"));
        catalog.put("morado", new Info("#800080", "2685 C"));
        catalog.put("naranja", new Info("#FFA500", "151 C"));
        catalog.put("malva", new Info("#E0B4D6", "2635 C"));
        catalog.put("cuarzo", new Info("#F5F5F5", "Cool Gray 1 C"));
        catalog.put("masilla", new Info("#D2B48C", "4695 C"));
        catalog.put("maravilla", new Info("#FFD700", "871 C"));
        
        Map<String, String> alias = new HashMap<>();
        alias.put("white", "blanco");
        alias.put("black", "negro");
        alias.put("gray", "gris");
        alias.put("plomo", "gris");
        alias.put("red", "rojo");
        alias.put("blue", "azul");
        alias.put("green", "verde");
        alias.put("brown", "marron");
        alias.put("purple", "purpura");
        alias.put("violeta", "purpura");
        alias.put("gold", "dorado");
        alias.put("silver", "plateado");
        alias.put("fucsia", "rosa");
        
        List<String> tokens = parseNombre(nombre);
        List<String> hexes = new ArrayList<>();
        List<String> pants = new ArrayList<>();
        
        for (String t : tokens) {
            String key = catalog.containsKey(t) ? t : alias.getOrDefault(t, null);
            if (key == null || !catalog.containsKey(key)) continue;
            Info info = catalog.get(key);
            hexes.add(info.hex);
            pants.add(info.pantone);
        }
        
        if (hexes.isEmpty()) {
            Map<String, String> defaultMap = new HashMap<>();
            defaultMap.put("codigo_hex", "#808080"); // Gris por defecto
            defaultMap.put("codigo_pantone", "Cool Gray 7 C");
            return defaultMap;
        }
        
        int r = 0, g = 0, b = 0;
        for (String h : hexes) {
            r += Integer.parseInt(h.substring(1, 3), 16);
            g += Integer.parseInt(h.substring(3, 5), 16);
            b += Integer.parseInt(h.substring(5, 7), 16);
        }
        int n = hexes.size();
        String hex = String.format("#%02X%02X%02X", Math.round(r / (float) n), Math.round(g / (float) n), Math.round(b / (float) n));
        String pantone = String.join("/", pants);
        
        String s = unaccent(nombre);
        if (s.contains("blanco") && s.contains("negro")) pantone = "GUIDE-BW";
        
        // Limitar la longitud del código pantone a 20 caracteres (límite de la BD)
        if (pantone != null && pantone.length() > 20) {
            pantone = pantone.substring(0, 20);
        }
        
        Map<String, String> out = new HashMap<>();
        out.put("codigo_hex", hex);
        out.put("codigo_pantone", pantone);
        return out;
    }
}