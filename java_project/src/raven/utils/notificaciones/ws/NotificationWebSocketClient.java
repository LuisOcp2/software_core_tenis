package raven.utils.notificaciones.ws;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.net.http.WebSocket.Listener;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import raven.controlador.admin.ModelNotificacion;

/**
 * Cliente WebSocket ligero para recibir notificaciones en tiempo real.
 * Sin dependencias externas: usa java.net.http.WebSocket (Java 11+).
 */
public class NotificationWebSocketClient implements Listener {
    private final String endpointUrl;
    private final Consumer<ModelNotificacion> onNotification;
    private final HttpClient httpClient;
    private WebSocket webSocket;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private volatile boolean closing = false;
    private int retrySeconds = 3;
    private final int maxRetrySeconds = 60;
    private final int userId;
    private final Integer bodegaId;

    public NotificationWebSocketClient(String endpointUrl, int userId, Integer bodegaId, Consumer<ModelNotificacion> onNotification) {
        this.endpointUrl = endpointUrl;
        this.onNotification = onNotification;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(6))
                .build();
        this.userId = userId;
        this.bodegaId = bodegaId;
    }

    public void connect() {
        closing = false;
        try {
            URI uri = URI.create(endpointUrl);
            httpClient.newWebSocketBuilder()
                    .buildAsync(uri, this)
                    .whenComplete((ws, err) -> {
                        if (err != null) {
                            scheduleReconnect();
                        } else {
                            this.webSocket = ws;
                            retrySeconds = 3;
                        }
                    });
        } catch (Exception ex) {
            scheduleReconnect();
        }
    }

    public void close() {
        closing = true;
        try {
            if (webSocket != null) {
                webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "bye");
            }
        } catch (Exception ignore) {}
        scheduler.shutdownNow();
    }

    @Override
    public void onOpen(WebSocket webSocket) {
        webSocket.request(1);
    }

    @Override
    public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
        try {
            String text = String.valueOf(data);
            ModelNotificacion notif = parseNotification(text);
            if (notif != null) {
                if (shouldDeliver(notif)) {
                    if (onNotification != null) {
                        onNotification.accept(notif);
                    }
                }
            }
        } catch (Exception ignore) {}
        webSocket.request(1);
        return CompletableFuture.completedStage(null);
    }

    private boolean shouldDeliver(ModelNotificacion n) {
        Integer dest = n.getIdUsuarioDestinatario();
        boolean forUser = dest != null && dest == userId;
        boolean forAll = n.isParaTodos();
        boolean forWarehouse = false;
        try {
            // Si el backend incluye bodega en el mensaje (extensión opcional)
            // aceptamos campo adicional "id_bodega" en el payload
            // Este método no puede leerlo desde ModelNotificacion; se filtra del lado del servidor.
            forWarehouse = true; // confiamos en el filtro del servidor
        } catch (Exception ignore) {}
        return forUser || (forAll && forWarehouse);
    }

    @Override
    public CompletionStage<?> onBinary(WebSocket webSocket, java.nio.ByteBuffer data, boolean last) {
        webSocket.request(1);
        return CompletableFuture.completedStage(null);
    }

    @Override
    public CompletionStage<?> onPing(WebSocket webSocket, java.nio.ByteBuffer message) {
        webSocket.sendPong(message);
        webSocket.request(1);
        return CompletableFuture.completedStage(null);
    }

    @Override
    public CompletionStage<?> onPong(WebSocket webSocket, java.nio.ByteBuffer message) {
        webSocket.request(1);
        return CompletableFuture.completedStage(null);
    }

    @Override
    public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
        if (!closing) {
            scheduleReconnect();
        }
        return CompletableFuture.completedStage(null);
    }

    @Override
    public void onError(WebSocket webSocket, Throwable error) {
        if (!closing) {
            scheduleReconnect();
        }
    }

    private void scheduleReconnect() {
        if (closing) return;
        int delay = retrySeconds;
        retrySeconds = Math.min(retrySeconds * 2, maxRetrySeconds);
        scheduler.schedule(this::connect, delay, TimeUnit.SECONDS);
    }

    /**
     * Parsing ligero de JSON (sin dependencias) para payloads sencillos.
     * Espera campos como:
     * { "id_notificacion":123, "titulo":"...", "mensaje":"...", "tipo":"INFO", "categoria":"inventario",
     *   "id_usuario_destinatario":5, "para_todos":false, "id_referencia":99, "tipo_referencia":"traspasos", "evento":"enviado",
     *   "fecha_creacion":"2025-01-10T12:34:56" }
     *
     * Si no es JSON, intenta formato key=value;key2=value2.
     */
    private ModelNotificacion parseNotification(String text) {
        try {
            Map<String, String> m = new HashMap<>();
            String s = text.trim();
            if (s.startsWith("{") && s.endsWith("}")) {
                // Parseo básico
                s = s.substring(1, s.length() - 1);
                // Separar por comas no anidadas (suficiente para payload plano)
                for (String part : s.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)")) {
                    String[] kv = part.split(":", 2);
                    if (kv.length == 2) {
                        String key = strip(kv[0]);
                        String val = strip(kv[1]);
                        m.put(key, val);
                    }
                }
            } else {
                for (String part : s.split(";")) {
                    String[] kv = part.split("=", 2);
                    if (kv.length == 2) {
                        m.put(strip(kv[0]), strip(kv[1]));
                    }
                }
            }
            int id = parseInt(m.getOrDefault("id_notificacion", m.getOrDefault("id", "0")));
            String titulo = m.getOrDefault("titulo", "");
            String mensaje = m.getOrDefault("mensaje", "");
            String tipo = m.getOrDefault("tipo", "INFO");
            String categoria = m.getOrDefault("categoria", "sistema");
            Integer idUsuario = parseNullableInt(m.get("id_usuario_destinatario"));
            boolean paraTodos = parseBoolean(m.getOrDefault("para_todos", "false"));
            Integer idRef = parseNullableInt(m.get("id_referencia"));
            String tipoRef = m.getOrDefault("tipo_referencia", "");
            boolean leida = false;
            boolean activa = true;
            java.time.LocalDateTime fc = null;
            try {
                String fcs = m.get("fecha_creacion");
                if (fcs != null && !fcs.isEmpty()) fc = java.time.LocalDateTime.parse(fcs);
            } catch (Exception ignore) {}
            ModelNotificacion n = new ModelNotificacion(id, titulo, mensaje, tipo, categoria, idUsuario, paraTodos, idRef, tipoRef, leida, activa, fc, null);
            String ev = m.get("evento");
            if (ev != null) n.setEvento(ev);
            return n;
        } catch (Exception e) {
            return null;
        }
    }

    private static String strip(String s) {
        if (s == null) return "";
        s = s.trim();
        if (s.startsWith("\"") && s.endsWith("\"")) {
            s = s.substring(1, s.length() - 1);
        }
        return s;
    }

    private static int parseInt(String s) {
        try { return Integer.parseInt(s); } catch (Exception e) { return 0; }
    }
    private static Integer parseNullableInt(String s) {
        try { return s != null ? Integer.valueOf(s) : null; } catch (Exception e) { return null; }
    }
    private static boolean parseBoolean(String s) {
        if (s == null) return false;
        s = s.toLowerCase();
        return "true".equals(s) || "1".equals(s) || "yes".equals(s);
    }
}
