package raven.utils.tono;

import javazoom.jl.player.Player;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

/**
 * Reproductor de sonidos para notificaciones del sistema.
 * Soporta archivos MP3 usando la librería JLayer.
 */
public class CorporateTone {

    // ====================================================================
    // CONFIGURACIÓN: Ruta del archivo MP3
    // ====================================================================

    // Tu archivo MP3 en la misma carpeta que esta clase
    private static final String MP3_FILE = "tono_noti.mp3";

    // Ruta completa (alternativa si quieres usar ruta absoluta)
    // private static final String MP3_ABSOLUTE_PATH = "C:/MisArchivos/tono_noti.mp3";

    // Control de volumen (0.0 a 1.0) - Nota: JLayer tiene limitaciones de volumen
    private static final boolean USE_ABSOLUTE_PATH = false;

    /**
     * Reproduce sonido de alerta (para eventos importantes)
     */
    public static void playAlert() {
        playMP3Sound();
    }

    /**
     * Reproduce sonido informativo (para eventos secundarios)
     */
    public static void playInfo() {
        playMP3Sound();
    }

    /**
     * Reproduce el archivo MP3 configurado
     */
    private static void playMP3Sound() {
        new Thread(() -> {
            try {
                InputStream inputStream = null;

                if (USE_ABSOLUTE_PATH) {
                    // Usar ruta absoluta si está configurada
                    // File file = new File(MP3_ABSOLUTE_PATH);
                    // inputStream = new FileInputStream(file);
                } else {
                    // Opción 1: Buscar el MP3 en la misma carpeta que esta clase
                    String packagePath = CorporateTone.class.getPackage().getName().replace('.', '/');
                    String mp3Path = "src/" + packagePath + "/" + MP3_FILE;

                    File mp3File = new File(mp3Path);

                    if (mp3File.exists()) {
                        // Archivo encontrado en el sistema de archivos
                        inputStream = new FileInputStream(mp3File);
                        System.out.println("SUCCESS  Reproduciendo: " + mp3File.getAbsolutePath());
                    } else {
                        // Intentar cargar como recurso (dentro del JAR)
                        String resourcePath = "/" + packagePath + "/" + MP3_FILE;
                        inputStream = CorporateTone.class.getResourceAsStream(resourcePath);

                        if (inputStream != null) {
                            System.out.println("SUCCESS  Reproduciendo desde recursos: " + resourcePath);
                        } else {
                            System.err.println("ERROR  No se encontró: " + mp3Path);
                            System.err.println("ERROR  Tampoco en recursos: " + resourcePath);
                            playFallbackBeep();
                            return;
                        }
                    }
                }

                if (inputStream != null) {
                    BufferedInputStream bufferedStream = new BufferedInputStream(inputStream);
                    Player player = new Player(bufferedStream);
                    player.play();
                    player.close();
                    bufferedStream.close();
                }

            } catch (Exception e) {
                System.err.println("ERROR  Error reproduciendo MP3: " + e.getMessage());
                e.printStackTrace();
                // Fallback a beep del sistema
                playFallbackBeep();
            }
        }, "MP3Player").start();
    }

    /**
     * Sonido de respaldo si falla la reproducción del MP3
     */
    private static void playFallbackBeep() {
        try {
            java.awt.Toolkit.getDefaultToolkit().beep();
        } catch (Exception ignore) {}
    }
}

