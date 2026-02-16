/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package raven.controlador.update;

import raven.controlador.principal.AppConfig;
import raven.application.Application;
import raven.controlador.principal.AppConfig;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.awt.Desktop;
import java.net.URI;

/**
 *
 * @author CrisDEV
 */

public class UpdateManager {

    // Método público que usarás desde tu aplicación
    public static void checkForUpdates() {
        try {
            // 1. Descargar el JSON desde tu servidor
            String json = downloadText(AppConfig.UPDATE_JSON_URL);
            if (json == null || json.isEmpty()) {
                System.out.println("No se pudo obtener el JSON de actualización");
                return;
            }

            String remoteVersion = extractValue(json, "version");
            if (remoteVersion == null) {
                System.out.println("JSON de actualización incompleto (versión)");
                return;
            }

            if (!isNewerVersion(remoteVersion, AppConfig.APP_VERSION)) {
                System.out.println("Ya tienes la última versión (" + AppConfig.APP_VERSION + ")");
                return;
            }

            String installerUrl  = extractValue(json, "url_instalador");
            String changelog     = extractValue(json, "changelog");
            if (installerUrl == null) {
                System.out.println("JSON de actualización incompleto (instalador)");
                return;
            }

            prepareInstallerInBackground(remoteVersion, changelog, installerUrl);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // === Métodos auxiliares ===

    // Descarga texto de una URL
    private static String downloadText(String urlString) throws Exception {
        try {
            return fetchText(urlString);
        } catch (javax.net.ssl.SSLHandshakeException ssl) {
            System.err.println("WARNING  Falló HTTPS por SSLHandshake: " + ssl.getMessage());
            if (urlString.startsWith("https://")) {
                String httpUrl = "http://" + urlString.substring("https://".length());
                System.err.println("→ Probando fallback inseguro HTTP: " + httpUrl);
                return fetchText(httpUrl);
            }
            throw ssl;
        }
    }

    private static String fetchText(String urlString) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(2500);
        conn.setReadTimeout(2500);
        conn.setRequestMethod("GET");
        conn.setInstanceFollowRedirects(true);
        conn.setRequestProperty("User-Agent", "ZapatosXtreme-Updater/1.0");
        conn.setRequestProperty("Accept", "application/json, text/plain, */*");
        int status = conn.getResponseCode();
        if (status != HttpURLConnection.HTTP_OK) {
            BufferedReader err = null;
            try {
                err = new BufferedReader(new InputStreamReader(conn.getErrorStream(), "UTF-8"));
                StringBuilder esb = new StringBuilder();
                String el; while (err != null && (el = err.readLine()) != null) { esb.append(el); }
                System.err.println("Error HTTP al obtener JSON: " + status + (esb.length() > 0 ? (" -> " + esb) : ""));
            } catch (Exception ignore) {}
            return null;
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) { sb.append(line); }
            return sb.toString();
        }
    }

    // Busca "clave": "valor" dentro del JSON (simple para este caso)
    private static String extractValue(String json, String key) {
        String pattern = "\"" + key + "\"";
        int index = json.indexOf(pattern);
        if (index == -1) return null;

        int colon = json.indexOf(":", index);
        if (colon == -1) return null;

        int firstQuote = json.indexOf("\"", colon + 1);
        if (firstQuote == -1) return null;

        int secondQuote = json.indexOf("\"", firstQuote + 1);
        if (secondQuote == -1) return null;

        return json.substring(firstQuote + 1, secondQuote);
    }

    // Compara versiones tipo "1.0.0" vs "1.0.1"
    private static boolean isNewerVersion(String remote, String local) {
        String[] r = remote.split("\\.");
        String[] l = local.split("\\.");

        int max = Math.max(r.length, l.length);
        for (int i = 0; i < max; i++) {
            int rv = (i < r.length) ? parseIntSafe(r[i]) : 0;
            int lv = (i < l.length) ? parseIntSafe(l[i]) : 0;

            if (rv > lv) return true;
            if (rv < lv) return false;
        }
        return false; // iguales
    }

    private static int parseIntSafe(String s) {
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    // Abre la URL del instalador en el navegador
    private static void openInstaller(String installerUrl) {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(new URI(installerUrl));
            } else {
                JOptionPane.showMessageDialog(null,
                        "No se pudo abrir el navegador. Descarga manualmente desde:\n" + installerUrl,
                        "Error al abrir instalador",
                        JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null,
                    "Error al abrir el instalador:\n" + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    // Descarga el instalador en la carpeta temporal y lo ejecuta
private static void downloadAndRunInstaller(String installerUrl) {
    try {
        String tmpDir = System.getProperty("java.io.tmpdir");
        String fileName = installerUrl.substring(installerUrl.lastIndexOf('/') + 1);
        java.io.File dest = new java.io.File(tmpDir, fileName);

        String normalized = normalizeUrl(installerUrl);
        java.net.URL url = new java.net.URL(normalized);
        java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);
        conn.setRequestMethod("GET");
        conn.setInstanceFollowRedirects(true);
        conn.setRequestProperty("User-Agent", "ZapatosXtreme-Updater/1.0");
        conn.setRequestProperty("Accept", "application/octet-stream, */*");

        int status = conn.getResponseCode();
        if (status != java.net.HttpURLConnection.HTTP_OK) {
            if (normalized.startsWith("https://")) {
                String httpUrl = "http://" + normalized.substring("https://".length());
                url = new java.net.URL(httpUrl);
                conn = (java.net.HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                conn.setRequestMethod("GET");
                conn.setInstanceFollowRedirects(true);
                conn.setRequestProperty("User-Agent", "ZapatosXtreme-Updater/1.0");
                conn.setRequestProperty("Accept", "application/octet-stream, */*");
                status = conn.getResponseCode();
            }
        }

        if (status != java.net.HttpURLConnection.HTTP_OK) {
            java.io.InputStream err = conn.getErrorStream();
            String msg = "HTTP " + status + " al descargar instalador";
            if (err != null) {
                try (java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(err))) {
                    StringBuilder sb = new StringBuilder(); String line; while ((line = br.readLine()) != null) sb.append(line);
                    if (sb.length() > 0) msg += ": " + sb.toString();
                }
            }
            throw new RuntimeException(msg + " -> " + normalized);
        }

        try (java.io.InputStream in = conn.getInputStream();
             java.io.FileOutputStream fos = new java.io.FileOutputStream(dest)) {
            byte[] buf = new byte[8192]; int r; while ((r = in.read(buf)) != -1) fos.write(buf, 0, r);
        }

        int r = JOptionPane.showConfirmDialog(
                null,
                "Se descargó el instalador en:\n" + dest.getAbsolutePath()
                        + "\n\nSe desinstalará la versión anterior (si existe) y se instalará la nueva.\n"
                        + "¿Deseas continuar?",
                "Instalador descargado",
                JOptionPane.YES_NO_OPTION
        );

        if (r == JOptionPane.YES_OPTION) {
            preUninstallPreviousVersions("Global Tennis", null);
            java.awt.Desktop.getDesktop().open(dest);
            System.exit(0);
        }

    } catch (Exception e) {
        JOptionPane.showMessageDialog(
                null,
                "Error al descargar o ejecutar el instalador:\n" + e.getMessage(),
                "Error en actualización",
                JOptionPane.ERROR_MESSAGE
        );
    }
}

// Desinstala versiones anteriores encontrando el desinstalador en el registro
private static void preUninstallPreviousVersions(String productNameContains, String newVersion) {
    try {
        java.util.List<String> keys = new java.util.ArrayList<>();
        keys.add("HKLM\\SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Uninstall");
        keys.add("HKLM\\SOFTWARE\\WOW6432Node\\Microsoft\\Windows\\CurrentVersion\\Uninstall");
        keys.add("HKCU\\SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Uninstall");

        for (String hive : keys) {
            java.util.List<String> subkeys = queryRegistrySubkeys(hive);
            for (String sub : subkeys) {
                java.util.Map<String,String> values = queryRegistryValues(hive + "\\" + sub);
                String name = values.getOrDefault("DisplayName", "");
                String uninstall = values.getOrDefault("UninstallString", "");
                if (name != null && name.toLowerCase().contains(productNameContains.toLowerCase())) {
                    String cmd = makeSilentUninstallCommand(uninstall);
                    if (cmd != null && !cmd.isEmpty()) {
                        try {
                            new ProcessBuilder("cmd.exe","/c",cmd).inheritIO().start().waitFor();
                        } catch (InterruptedException ie) { /* ignore */ }
                    }
                }
            }
        }
    } catch (Exception e) {
        System.err.println("No se pudo desinstalar versión anterior: " + e.getMessage());
    }
}

private static java.util.List<String> queryRegistrySubkeys(String path) throws Exception {
    java.util.List<String> list = new java.util.ArrayList<>();
    Process p = new ProcessBuilder("reg","query",path).start();
    try (java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(p.getInputStream()))) {
        String line; while ((line = br.readLine()) != null) {
            line = line.trim();
            if (line.startsWith(path + "\\")) list.add(line.substring(path.length()+1));
        }
    }
    return list;
}

private static java.util.Map<String,String> queryRegistryValues(String key) throws Exception {
    java.util.Map<String,String> map = new java.util.HashMap<>();
    Process p = new ProcessBuilder("reg","query",key).start();
    try (java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(p.getInputStream()))) {
        String line; while ((line = br.readLine()) != null) {
            line = line.trim();
            // Expected: <ValueName>    <Type>    <Data>
            String[] parts = line.split("\t");
            if (parts.length >= 3) map.put(parts[0].trim(), parts[2].trim());
        }
    }
    return map;
}

private static String makeSilentUninstallCommand(String uninstall) {
    if (uninstall == null || uninstall.isEmpty()) return null;
    String u = uninstall.trim();
    if (u.toLowerCase().contains("msiexec")) {
        // msiexec /x {GUID}
        return u + " /quiet /norestart";
    }
    if (u.toLowerCase().endsWith(".exe") || u.toLowerCase().contains("unins")) {
        if (u.toLowerCase().contains("/s") || u.toLowerCase().contains("/verysilent")) return u;
        // Try both common flags
        return u + " /VERYSILENT /SUPPRESSMSGBOXES /NORESTART";
    }
    return u;
}

private static String normalizeUrl(String url) {
    try {
        return new java.net.URL(url).toURI().toASCIIString();
    } catch (Exception e) {
        return url.replace(" ", "%20");
    }
}

    private static void showUpdateNotification(String remoteVersion, String changelog, String installerUrl) {
        SwingUtilities.invokeLater(() -> {
            JWindow win = new JWindow();
            win.setAlwaysOnTop(true);
            JPanel content = new JPanel(new BorderLayout(12, 8));
            content.setBorder(BorderFactory.createCompoundBorder(
                    new javax.swing.border.LineBorder(new java.awt.Color(0,0,0,60), 1, true),
                    BorderFactory.createEmptyBorder(12, 14, 12, 14)));
            content.setBackground(new java.awt.Color(40, 45, 52));

            JLabel icon = new JLabel(UIManager.getIcon("OptionPane.informationIcon"));
            content.add(icon, BorderLayout.WEST);

            String body = "Nueva versión disponible: " + remoteVersion
                    + "<br>Actual: " + AppConfig.APP_VERSION
                    + (changelog != null && !changelog.isEmpty() ? ("<br>" + changelog) : "");
            JLabel txt = new JLabel("<html><div style='color:#ffffff; font-family:Segoe UI, sans-serif;'>" + body + "</div></html>");
            content.add(txt, BorderLayout.CENTER);

            JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
            actions.setOpaque(false);
            JButton btnUpdate = new JButton("Actualizar");
            JButton btnClose = new JButton("Cerrar");
            actions.add(btnClose);
            actions.add(btnUpdate);
            content.add(actions, BorderLayout.SOUTH);

            btnClose.addActionListener(e -> win.dispose());
            btnUpdate.addActionListener(e -> {
                win.dispose();
                downloadAndRunInstaller(installerUrl);
            });

            win.getContentPane().add(content);
            win.pack();
            java.awt.Dimension s = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
            int x = s.width - win.getWidth() - 24;
            int y = s.height - win.getHeight() - 48;
            win.setLocation(x, y);
            win.setVisible(true);

            new javax.swing.Timer(20000, ev -> win.dispose()){{setRepeats(false);}}.start();
        });
    }

    private static volatile String preparedInstallerPath;
    private static volatile String preparedVersion;

    public static boolean hasPreparedUpdate() {
        return preparedInstallerPath != null;
    }

    public static void triggerImmediateUpdate() {
        if (preparedInstallerPath != null) {
            int r = javax.swing.JOptionPane.showConfirmDialog(null, "Se cerrará el sistema y se instalará la actualización. ¿Continuar?", "Actualizar", javax.swing.JOptionPane.YES_NO_OPTION);
            if (r == javax.swing.JOptionPane.YES_OPTION) {
                preUninstallPreviousVersions("Global Tennis", null);
                try { java.awt.Desktop.getDesktop().open(new java.io.File(preparedInstallerPath)); } catch (Exception ignore) {}
                System.exit(0);
            }
            return;
        }
        try {
            String json = downloadText(AppConfig.UPDATE_JSON_URL);
            if (json == null || json.isEmpty()) return;
            String remoteVersion = extractValue(json, "version");
            if (remoteVersion == null) return;
            if (!isNewerVersion(remoteVersion, AppConfig.APP_VERSION)) {
                javax.swing.JOptionPane.showMessageDialog(null, "No hay actualizaciones disponibles", "Actualizar", javax.swing.JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            String installerUrl  = extractValue(json, "url_instalador");
            if (installerUrl == null) return;
            downloadAndRunInstaller(installerUrl);
        } catch (Exception ignore) {}
    }

    private static void prepareInstallerInBackground(String remoteVersion, String changelog, String installerUrl) {
        new javax.swing.SwingWorker<java.io.File, Void>() {
            @Override protected java.io.File doInBackground() throws Exception {
                String tmpDir = System.getProperty("java.io.tmpdir");
                String fileName = installerUrl.substring(installerUrl.lastIndexOf('/') + 1);
                java.io.File dest = new java.io.File(tmpDir, fileName);
                String normalized = normalizeUrl(installerUrl);
                java.net.URL url = new java.net.URL(normalized);
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                conn.setRequestMethod("GET");
                conn.setInstanceFollowRedirects(true);
                conn.setRequestProperty("User-Agent", "ZapatosXtreme-Updater/1.0");
                conn.setRequestProperty("Accept", "application/octet-stream, */*");
                int status = conn.getResponseCode();
                if (status != java.net.HttpURLConnection.HTTP_OK) return null;
                try (java.io.InputStream in = conn.getInputStream(); java.io.FileOutputStream fos = new java.io.FileOutputStream(dest)) {
                    byte[] buf = new byte[8192]; int r; while ((r = in.read(buf)) != -1) fos.write(buf, 0, r);
                }
                return dest;
            }
            @Override protected void done() {
                try {
                    java.io.File f = get();
                    if (f != null && f.exists()) {
                        preparedInstallerPath = f.getAbsolutePath();
                        preparedVersion = remoteVersion;
                        showPreparedUpdateNotification(remoteVersion, changelog);
                        try {
                            if (Application.app != null) {
                                Application.app.getMainForm().getMenu().setUpdateItemVisible(true);
                            }
                        } catch (Throwable ignore) {}
                    }
                } catch (Exception ignore) {}
            }
        }.execute();
    }

    private static void showPreparedUpdateNotification(String remoteVersion, String changelog) {
        SwingUtilities.invokeLater(() -> {
            JWindow win = new JWindow();
            win.setAlwaysOnTop(true);
            JPanel content = new JPanel(new BorderLayout(12, 8));
            content.setBorder(BorderFactory.createCompoundBorder(new javax.swing.border.LineBorder(new java.awt.Color(0,0,0,60), 1, true), BorderFactory.createEmptyBorder(12, 14, 12, 14)));
            content.setBackground(new java.awt.Color(40, 45, 52));
            JLabel icon = new JLabel(UIManager.getIcon("OptionPane.informationIcon"));
            content.add(icon, BorderLayout.WEST);
            String body = "Actualización preparada: " + remoteVersion + "<br>Actual: " + AppConfig.APP_VERSION + (changelog != null && !changelog.isEmpty() ? ("<br>" + changelog) : "");
            JLabel txt = new JLabel("<html><div style='color:#ffffff; font-family:Segoe UI, sans-serif;'>" + body + "</div></html>");
            content.add(txt, BorderLayout.CENTER);
            JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
            actions.setOpaque(false);
            JButton btnInstall = new JButton("Instalar ahora");
            JButton btnClose = new JButton("Cerrar");
            actions.add(btnClose);
            actions.add(btnInstall);
            content.add(actions, BorderLayout.SOUTH);
            btnClose.addActionListener(e -> win.dispose());
            btnInstall.addActionListener(e -> {
                win.dispose();
                int r = JOptionPane.showConfirmDialog(null, "Se cerrará el sistema para instalar la actualización. ¿Continuar?", "Actualizar", JOptionPane.YES_NO_OPTION);
                if (r == JOptionPane.YES_OPTION) {
                    preUninstallPreviousVersions("Global Tennis", null);
                    try { java.awt.Desktop.getDesktop().open(new java.io.File(preparedInstallerPath)); } catch (Exception ignore) {}
                    System.exit(0);
                }
            });
            win.getContentPane().add(content);
            win.pack();
            java.awt.Dimension s = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
            int x = s.width - win.getWidth() - 24;
            int y = s.height - win.getHeight() - 48;
            win.setLocation(x, y);
            win.setVisible(true);
            new javax.swing.Timer(20000, ev -> win.dispose()){{setRepeats(false);}}.start();
        });
    }
}