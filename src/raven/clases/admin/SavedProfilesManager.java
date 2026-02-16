package raven.clases.admin;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.prefs.Preferences;
import raven.controlador.admin.ModelUser;

/**
 * Gestión de perfiles guardados que muestra nombres en lugar de usernames
 * pero mantiene la validación por username en segundo plano
 */
public class SavedProfilesManager {
    
    private static final String SAVED_PROFILES_NODE = "raven/login_profiles";
    private static final String SAVED_PROFILES_KEY = "saved_profiles_with_names";
    private static final String PROFILE_IMAGE_PREFIX = "profile_image_path_";
    
    /**
     * Guarda un perfil con nombre y username
     */
    public static void saveProfile(String username, String nombre) {
        String profileData = username + "|" + nombre;
        
        try {
            Preferences prefs = Preferences.userRoot().node(SAVED_PROFILES_NODE);
            String raw = prefs.get(SAVED_PROFILES_KEY, "");
            
            // Dividir perfiles existentes
            String[] existingProfiles = raw.isEmpty() ? new String[0] : raw.split("\\R+");
            LinkedHashSet<String> profilesSet = new LinkedHashSet<>();
            
            // Agregar el nuevo perfil al inicio
            profilesSet.add(profileData);
            
            // Agregar perfiles existentes (máximo 8)
            for (String profile : existingProfiles) {
                if (profile != null && !profile.trim().isEmpty() && !profilesSet.contains(profile)) {
                    profilesSet.add(profile);
                    if (profilesSet.size() >= 8) break;
                }
            }
            
            // Convertir de nuevo a string
            StringBuilder sb = new StringBuilder();
            int count = 0;
            for (String profile : profilesSet) {
                if (count > 0) sb.append('\n');
                sb.append(profile);
                count++;
                if (count >= 8) break;
            }
            
            prefs.put(SAVED_PROFILES_KEY, sb.toString());
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Carga los perfiles guardados
     */
    public static List<SavedProfile> loadSavedProfiles() {
        List<SavedProfile> profiles = new ArrayList<>();
        
        try {
            Preferences prefs = Preferences.userRoot().node(SAVED_PROFILES_NODE);
            String raw = prefs.get(SAVED_PROFILES_KEY, "");
            
            if (!raw.isEmpty()) {
                String[] profileLines = raw.split("\\R+");
                LinkedHashSet<String> uniqueProfiles = new LinkedHashSet<>();
                
                for (String line : profileLines) {
                    String profile = line == null ? "" : line.trim();
                    if (!profile.isEmpty() && !uniqueProfiles.contains(profile)) {
                        uniqueProfiles.add(profile);
                        
                        // Separar username y nombre
                        String[] parts = profile.split("\\|", 2);
                        if (parts.length >= 2) {
                            profiles.add(new SavedProfile(parts[0].trim(), parts[1].trim()));
                        } else if (parts.length >= 1) {
                            // Si solo hay username, usarlo como nombre también
                            String username = parts[0].trim();
                            profiles.add(new SavedProfile(username, username));
                        }
                        
                        if (profiles.size() >= 8) break;
                    }
                }
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return profiles;
    }
    
    /**
     * Elimina un perfil guardado
     */
    public static void removeProfile(String username) {
        try {
            Preferences prefs = Preferences.userRoot().node(SAVED_PROFILES_NODE);
            String raw = prefs.get(SAVED_PROFILES_KEY, "");
            
            if (!raw.isEmpty()) {
                String[] profileLines = raw.split("\\R+");
                List<String> remainingProfiles = new ArrayList<>();
                
                for (String line : profileLines) {
                    String profile = line == null ? "" : line.trim();
                    if (!profile.isEmpty()) {
                        String[] parts = profile.split("\\|", 2);
                        if (parts.length >= 1) {
                            String profileUsername = parts[0].trim();
                            if (!profileUsername.equals(username)) {
                                remainingProfiles.add(profile);
                            }
                        }
                    }
                }
                
                // Convertir de nuevo a string
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < remainingProfiles.size(); i++) {
                    if (i > 0) sb.append('\n');
                    sb.append(remainingProfiles.get(i));
                }
                
                prefs.put(SAVED_PROFILES_KEY, sb.toString());
            }
            removeProfileImagePath(username);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void saveProfileImagePath(String username, String imagePath) {
        if (username == null || username.isBlank()) {
            return;
        }
        try {
            Preferences prefs = Preferences.userRoot().node(SAVED_PROFILES_NODE);
            if (imagePath == null || imagePath.isBlank()) {
                prefs.remove(keyForImage(username));
            } else {
                prefs.put(keyForImage(username), imagePath.trim());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String getProfileImagePath(String username) {
        if (username == null || username.isBlank()) {
            return "";
        }
        try {
            Preferences prefs = Preferences.userRoot().node(SAVED_PROFILES_NODE);
            return prefs.get(keyForImage(username), "");
        } catch (Exception e) {
            return "";
        }
    }

    public static void removeProfileImagePath(String username) {
        saveProfileImagePath(username, "");
    }

    private static String keyForImage(String username) {
        String u = username.trim().toLowerCase();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < u.length(); i++) {
            char c = u.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_' || c == '-') {
                sb.append(c);
            } else {
                sb.append('_');
            }
        }
        return PROFILE_IMAGE_PREFIX + sb;
    }
    
    /**
     * Clase para representar un perfil guardado
     */
    public static class SavedProfile {
        private String username;
        private String nombre;
        
        public SavedProfile(String username, String nombre) {
            this.username = username;
            this.nombre = nombre;
        }
        
        public String getUsername() {
            return username;
        }
        
        public String getNombre() {
            return nombre;
        }
    }
}
