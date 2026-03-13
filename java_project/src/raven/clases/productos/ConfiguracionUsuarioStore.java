package raven.clases.productos;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class ConfiguracionUsuarioStore {
    private static File getDir() {
        String home = System.getProperty("user.home");
        File dir = new File(home, ".zapatosRotulacion");
        if (!dir.exists()) dir.mkdirs();
        return dir;
    }
    private static File getFile() {
        return new File(getDir(), "configs.jsonl");
    }

    public static synchronized List<ConfiguracionBarTender> cargar() {
        List<ConfiguracionBarTender> list = new ArrayList<>();
        File f = getFile();
        if (!f.exists()) return list;
        try {
            for (String line : Files.readAllLines(f.toPath(), StandardCharsets.UTF_8)) {
                String s = line.trim();
                if (s.isEmpty()) continue;
                try {
                    ConfiguracionBarTender c = ConfiguracionBarTender.crearDesdeJson(s);
                    if (c != null) list.add(c);
                } catch (Exception ignore) {}
            }
        } catch (Exception ignore) {}
        return list;
    }

    public static synchronized void guardar(ConfiguracionBarTender cfg) {
        List<ConfiguracionBarTender> list = cargar();
        List<ConfiguracionBarTender> out = new ArrayList<>();
        boolean replaced = false;
        for (ConfiguracionBarTender c : list) {
            if (c.nombre != null && cfg.nombre != null && c.nombre.equals(cfg.nombre)) {
                out.add(cfg);
                replaced = true;
            } else {
                out.add(c);
            }
        }
        if (!replaced) out.add(cfg);
        guardarTodos(out);
    }

    public static synchronized void guardarTodos(List<ConfiguracionBarTender> list) {
        File f = getFile();
        try (BufferedWriter bw = Files.newBufferedWriter(f.toPath(), StandardCharsets.UTF_8)) {
            for (ConfiguracionBarTender c : list) {
                String json = c.convertirAJson();
                bw.write(json);
                bw.write("\n");
            }
        } catch (Exception ignore) {}
    }
}