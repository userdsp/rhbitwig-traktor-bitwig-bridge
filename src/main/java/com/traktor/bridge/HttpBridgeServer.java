package com.traktor.bridge;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

public class HttpBridgeServer {
    private final DeckManager deckManager;
    private HttpServer server;
    private boolean isRunning;
    
    public HttpBridgeServer(final DeckManager deckManager) {
        this.deckManager = deckManager;
        this.isRunning = false;
    }

    public void start() {
        try {
            server = HttpServer.create(new InetSocketAddress(BridgeAddress.PORT), 0);
            server.createContext("/", new TraktorApiHandler());
            server.setExecutor(null);
            server.start();
            isRunning = true;
            deckManager.logBridgeEvent("HTTP listener ready on port " + BridgeAddress.PORT);
        } catch (final IOException e) {
            isRunning = false;
            deckManager.logBridgeEvent("HTTP listener FAILED on port " + BridgeAddress.PORT + ": " + e.getMessage());
        }
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
            isRunning = false;
        }
    }

    public boolean isRunning() {
        return isRunning;
    }

    static boolean isElapsedTimeOnly(final JSONObject deckState) {
        return deckState.length() == 1 && deckState.has("elapsedTime");
    }

    private class TraktorApiHandler implements HttpHandler {
        @Override
        public void handle(final HttpExchange exchange) throws IOException {
            final String path = exchange.getRequestURI().getPath();
            final String method = exchange.getRequestMethod();

            if (!"POST".equalsIgnoreCase(method)) {
                sendResponse(exchange, 405, "Method Not Allowed");
                return;
            }

            final InputStream requestBody = exchange.getRequestBody();
            final String body = new String(requestBody.readAllBytes(), StandardCharsets.UTF_8);

            if (!path.startsWith("/updateDeck/") && !path.startsWith("/activeDeck/")
                    && !path.startsWith("/deckKey/")) {
                deckManager.logBridgeEvent("HTTP POST " + path + " body=" + body);
            }

            if (path.startsWith("/deckLoaded/")) {
                handleDeckLoaded(path, body);
            } else if (path.startsWith("/updateDeck/")) {
                handleDeckUpdate(path, body);
            } else if (path.startsWith("/activeDeck/")) {
                handleActiveDeck(path);
            } else if (path.startsWith("/deckKey/")) {
                handleDeckKey(path, body);
            } else if (path.startsWith("/tripleClickDeck/")) {
                handleTripleClickDeck(path);
            }

            sendResponse(exchange, 200, "OK");
        }

        private void handleDeckLoaded(final String path, final String body) {
            final int deckIndex = extractDeckIndex(path);
            
            try {
                final JSONObject trackData = new JSONObject(body);
                String key = extractTrackKeyFromJson(trackData);
                
                if (key != null && !key.isEmpty() && !"null".equals(key)) {
                    String cleanKey = key.replace("~", "").trim();
                    deckManager.logBridgeEvent("deck " + deckIndex + " loaded key payload=" + cleanKey);
                    deckManager.setDeckKey(deckIndex - 1, cleanKey);
                    deckManager.handleDeckLoaded(deckIndex - 1);
                }
            } catch (Exception e) {
                deckManager.logBridgeEvent("deckLoaded parse error: " + e.getMessage());
            }
        }

        private void handleDeckUpdate(final String path, final String body) {
            final int deckIndex = extractDeckIndex(path);
            
            try {
                final JSONObject deckState = new JSONObject(body);
                if (!isElapsedTimeOnly(deckState)) {
                    deckManager.logBridgeEvent("HTTP POST " + path + " body=" + body);
                }
                String key = extractTrackKeyFromJson(deckState);

                if (key != null && !key.isEmpty() && !"null".equals(key)) {
                    String cleanKey = key.replace("~", "").trim();
                    deckManager.logBridgeEvent("deck " + deckIndex + " analyzed track key update=" + cleanKey);
                    deckManager.setDeckKey(deckIndex - 1, cleanKey);
                } else if (deckState.has("resultingKey")) {
                    deckManager.logBridgeEvent("deck " + deckIndex + " ignored resultingKey="
                            + deckState.optString("resultingKey", "")
                            + " (pitch-shifted value; keeping analyzed track key)");
                }
            } catch (Exception e) {
                deckManager.logBridgeEvent("updateDeck parse error: " + e.getMessage());
            }

        }

        private void handleActiveDeck(final String path) {
            final int deckIndex = extractDeckIndex(path);
            deckManager.activateDeck(deckIndex - 1);
        }

        private void handleDeckKey(final String path, final String body) {
            final int deckIndex = extractDeckIndex(path);
            try {
                final String key = extractTrackKeyFromJson(new JSONObject(body));
                if (key != null) {
                    deckManager.setDeckKey(deckIndex - 1, key);
                }
            } catch (final org.json.JSONException e) {
                deckManager.logBridgeEvent("deckKey parse error: " + e.getMessage());
            }
        }

        private void handleTripleClickDeck(final String path) {
            final int deckIndex = extractDeckIndex(path);
            deckManager.logBridgeEvent("triple-click request=" + path);
            deckManager.activateDeckTripleClick(deckIndex - 1);
        }

        private String extractTrackKeyFromJson(JSONObject json) {
            // Only use track metadata. resultingKey changes with pitch/key shift
            // and is not the stable analyzed key shown in Traktor's library.
            final String[] fields = {"key", "keyText", "key_text", "track_key"};
            String fallback = null;
            for (String field : fields) {
                String key = json.optString(field, null);
                if (key == null || key.isBlank() || "null".equalsIgnoreCase(key)) continue;
                key = key.trim();
                if (key.matches("(?i)\\d+[dmab]")) return key;
                if (fallback == null) fallback = key;
            }
            return fallback;
        }

        private int extractDeckIndex(final String path) {
            final String[] parts = path.split("/");
            String lastPart = parts[parts.length - 1];
            return switch (lastPart.toUpperCase()) {
                case "A" -> 1;
                case "B" -> 2;
                case "C" -> 3;
                case "D" -> 4;
                default -> 1;
            };
        }

        private void sendResponse(final HttpExchange exchange, final int statusCode, final String response) throws IOException {
            exchange.sendResponseHeaders(statusCode, response.getBytes().length);
            final OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }
}
