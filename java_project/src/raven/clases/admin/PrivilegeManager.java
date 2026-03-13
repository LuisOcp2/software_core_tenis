package raven.clases.admin;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.io.*;
import raven.controlador.principal.conexion;

/**
 * Gestor de privilegios basado en base de datos.
 * 
 * Implementa:
 * - Cache de privilegios en memoria
 * - Recarga automática desde BD
 * - Control granular de permisos (ver, crear, editar, eliminar)
 * 
 * @author CrisDEV
 */
public class PrivilegeManager {
    
    private static PrivilegeManager instance;
    
    // Cache de privilegios: Map<rol, Map<modulo, permisos>>
    private final Map<String, Map<String, ModulePermissions>> privilegeCache;
    
    // Timestamp de última carga
    private long lastLoadTime;
    
    // Tiempo de validez del cache (5 minutos)
    private static final long CACHE_VALIDITY_MS = 5 * 60 * 1000;
    private int lastFingerprintCount = -1;
    private long lastFingerprintSum = -1L;
    
    // ===================================================================
    // SINGLETON
    // ===================================================================
    
    private PrivilegeManager() {
        this.privilegeCache = new HashMap<>();
        this.lastLoadTime = 0;
        if (!loadFromDisk()) {
            loadPrivileges();
            saveToDisk();
        }
    }
    
    public static synchronized PrivilegeManager getInstance() {
        if (instance == null) {
            instance = new PrivilegeManager();
        }
        return instance;
    }
    
    // ===================================================================
    // CLASE INTERNA: ModulePermissions
    // ===================================================================
    
    /**
     * Representa los permisos para un módulo específico.
     */
    public static class ModulePermissions {
        private boolean canView;
        private boolean canCreate;
        private boolean canEdit;
        private boolean canDelete;
        
        public ModulePermissions(boolean canView, boolean canCreate, 
                                boolean canEdit, boolean canDelete) {
            this.canView = canView;
            this.canCreate = canCreate;
            this.canEdit = canEdit;
            this.canDelete = canDelete;
        }
        
        public boolean canView() { return canView; }
        public boolean canCreate() { return canCreate; }
        public boolean canEdit() { return canEdit; }
        public boolean canDelete() { return canDelete; }
        
        @Override
        public String toString() {
            return String.format("Permissions{V:%s, C:%s, E:%s, D:%s}",
                canView, canCreate, canEdit, canDelete);
        }
    }
    
    // ===================================================================
    // MÉTODOS PÚBLICOS
    // ===================================================================
    
    /**
     * Verifica si un rol tiene permiso para VER un módulo.
     */
    public boolean canView(String role, String module) {
        return hasPermission(role, module, "view");
    }
    
    /**
     * Verifica si un rol tiene permiso para CREAR en un módulo.
     */
    public boolean canCreate(String role, String module) {
        return hasPermission(role, module, "create");
    }
    
    /**
     * Verifica si un rol tiene permiso para EDITAR en un módulo.
     */
    public boolean canEdit(String role, String module) {
        return hasPermission(role, module, "edit");
    }
    
    /**
     * Verifica si un rol tiene permiso para ELIMINAR en un módulo.
     */
    public boolean canDelete(String role, String module) {
        return hasPermission(role, module, "delete");
    }
    
    /**
     * Obtiene todos los permisos para un rol y módulo.
     */
    public ModulePermissions getPermissions(String role, String module) {
        refreshCacheIfNeeded();
        
        role = normalizeString(role);
        module = normalizeString(module);
        
        Map<String, ModulePermissions> rolePrivileges = privilegeCache.get(role);
        
        if (rolePrivileges != null) {
            ModulePermissions perms = rolePrivileges.get(module);
            if (perms != null) {
                return perms;
            }
        }
        
        // Sin permisos por defecto
        return new ModulePermissions(false, false, false, false);
    }
    
    /**
     * Recarga los privilegios desde la base de datos.
     */
    public synchronized void reload() {
        System.out.println("Actualizando Recargando privilegios desde BD...");
        privilegeCache.clear();
        loadPrivileges();
        saveToDisk();
    }
    
    // ===================================================================
    // MÉTODOS PRIVADOS
    // ===================================================================
    
    /**
     * Verifica un permiso específico.
     */
    private boolean hasPermission(String role, String module, String permission) {
        ModulePermissions perms = getPermissions(role, module);
        
        switch (permission.toLowerCase()) {
            case "view":
                return perms.canView();
            case "create":
                return perms.canCreate();
            case "edit":
                return perms.canEdit();
            case "delete":
                return perms.canDelete();
            default:
                return false;
        }
    }
    
    /**
     * Refresca el cache si ha expirado.
     */
    private void refreshCacheIfNeeded() {
        long currentTime = System.currentTimeMillis();
        
        if (currentTime - lastLoadTime > CACHE_VALIDITY_MS) {
            int[] fp = fetchFingerprint();
            if (fp != null) {
                if (fp[0] != lastFingerprintCount || fp[1] != lastFingerprintSum) {
                    reload();
                } else {
                    lastLoadTime = currentTime;
                }
            } else {
                reload();
            }
        }
    }
    
    /**
     * Carga los privilegios desde la base de datos.
     */
    private void loadPrivileges() {
        String sql = "SELECT rol, modulo, puede_ver, puede_crear, puede_editar, puede_eliminar " +
                    "FROM privilegios_rol ORDER BY rol, modulo";
        
        try (Connection con = conexion.getInstance().createConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            
            int count = 0;
            long sum = 0L;
            while (rs.next()) {
                String rol = normalizeString(rs.getString("rol"));
                String modulo = normalizeString(rs.getString("modulo"));
                
                boolean canView = rs.getBoolean("puede_ver");
                boolean canCreate = rs.getBoolean("puede_crear");
                boolean canEdit = rs.getBoolean("puede_editar");
                boolean canDelete = rs.getBoolean("puede_eliminar");
                
                ModulePermissions perms = new ModulePermissions(
                    canView, canCreate, canEdit, canDelete
                );
                
                // Agregar al cache
                privilegeCache
                    .computeIfAbsent(rol, k -> new HashMap<>())
                    .put(modulo, perms);
                
                count++;
                sum += (canView ? 1 : 0) + (canCreate ? 2 : 0) + (canEdit ? 4 : 0) + (canDelete ? 8 : 0);
            }
            
            lastLoadTime = System.currentTimeMillis();
            lastFingerprintCount = count;
            lastFingerprintSum = sum;
            
            System.out.println("SUCCESS  Privilegios cargados: " + count + " registros");
            System.out.println("   Roles: " + privilegeCache.keySet());
            
        } catch (SQLException e) {
            System.err.println("ERROR  Error cargando privilegios: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Normaliza strings para comparación (lowercase, trim).
     */
    private String normalizeString(String str) {
        if (str == null) {
            return "";
        }
        return str.toLowerCase().trim();
    }

    private File getCacheFile() {
        String home = System.getProperty("user.home", ".");
        File dir = new File(home, ".xtreme_cache");
        if (!dir.exists()) dir.mkdirs();
        return new File(dir, "privileges_cache.ser");
    }

    private boolean loadFromDisk() {
        File f = getCacheFile();
        if (!f.exists()) return false;
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(f))) {
            Object obj = ois.readObject();
            if (!(obj instanceof CacheRecord)) return false;
            CacheRecord rec = (CacheRecord) obj;
            this.privilegeCache.clear();
            this.privilegeCache.putAll(rec.cache);
            this.lastLoadTime = rec.lastLoad;
            this.lastFingerprintCount = rec.fpc;
            this.lastFingerprintSum = rec.fps;
            return true;
        } catch (Exception ignore) {
            return false;
        }
    }

    private void saveToDisk() {
        CacheRecord rec = new CacheRecord();
        rec.cache = this.privilegeCache;
        rec.lastLoad = this.lastLoadTime;
        rec.fpc = this.lastFingerprintCount;
        rec.fps = this.lastFingerprintSum;
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(getCacheFile()))) {
            oos.writeObject(rec);
        } catch (Exception ignore) {}
    }

    private int[] fetchFingerprint() {
        String sql = "SELECT COUNT(*) AS cnt, SUM((puede_ver=1) + (puede_crear=1) + (puede_editar=1) + (puede_eliminar=1)) AS s " +
                     "FROM privilegios_rol";
        try (Connection con = conexion.getInstance().createConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                int cnt = rs.getInt("cnt");
                long s = rs.getLong("s");
                return new int[]{cnt, (int) s};
            }
        } catch (SQLException ignore) {}
        return null;
    }

    public synchronized void invalidate() {
        privilegeCache.clear();
        lastLoadTime = 0;
    }

    private static class CacheRecord implements Serializable {
        Map<String, Map<String, ModulePermissions>> cache;
        long lastLoad;
        int fpc;
        long fps;
    }
    
    // ===================================================================
    // MÉTODOS DE DEBUG
    // ===================================================================
    
    /**
     * Imprime todos los privilegios del cache (para debugging).
     */
    public void printAllPrivileges() {
        System.out.println("\n=== PRIVILEGIOS CARGADOS ===");
        
        privilegeCache.forEach((rol, modules) -> {
            System.out.println("\n Rol: " + rol.toUpperCase());
            
            modules.forEach((module, perms) -> {
                System.out.println("   " + module + ": " + perms);
            });
        });
        
        System.out.println("\n============================\n");
    }
}
