package com.uit.feature.login.infrastructure;

import com.uit.shared.exception.AppException;

import java.awt.Desktop;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

public final class GoogleOAuthClient {

    private static final String SUCCESS_PAGE = """
            <!DOCTYPE html>
            <html><head><meta charset="utf-8"><title>UIT System</title></head>
            <body style="font-family:Segoe UI,sans-serif;background:#eef1f4;color:#0f172a;display:flex;min-height:100vh;align-items:center;justify-content:center;margin:0">
            <div style="background:#fff;border:1px solid #e2e8f0;border-radius:16px;padding:32px 36px;max-width:420px">
            <h1 style="font-size:22px;margin:0 0 8px">Return to UIT System</h1>
            <p style="margin:0;color:#475569">You can close this browser tab.</p>
            </div></body></html>
            """;

    private static final String CANCELLED_PAGE = """
            <!DOCTYPE html>
            <html><head><meta charset="utf-8"><title>UIT System</title></head>
            <body style="font-family:Segoe UI,sans-serif;background:#eef1f4;color:#0f172a;display:flex;min-height:100vh;align-items:center;justify-content:center;margin:0">
            <div style="background:#fff;border:1px solid #e2e8f0;border-radius:16px;padding:32px 36px;max-width:420px">
            <h1 style="font-size:22px;margin:0 0 8px">Sign-in cancelled</h1>
            <p style="margin:0;color:#475569">Return to UIT System and try again.</p>
            </div></body></html>
            """;

    private final GoogleAuthConfig config;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();
    private final AtomicReference<CompletableFuture<Map<String, String>>> pending = new AtomicReference<>();
    private final AtomicReference<com.sun.net.httpserver.HttpServer> server = new AtomicReference<>();

    public GoogleOAuthClient(GoogleAuthConfig config) {
        this.config = config;
    }

    public boolean isConfigured() {
        return config.configured();
    }

    public void cancel() {
        CompletableFuture<Map<String, String>> current = pending.getAndSet(null);
        if (current != null) {
            current.completeExceptionally(new AppException("Google sign-in was cancelled."));
        }
        stopServer();
    }

    public String requestEmail() {
        if (!config.configured()) {
            throw new AppException("Google sign-in is not set up yet. Add your desktop OAuth client id to " + config.file() + ".");
        }
        String state = randomToken();
        String verifier = randomToken();
        String challenge = codeChallenge(verifier);
        CompletableFuture<Map<String, String>> callback = new CompletableFuture<>();
        pending.set(callback);
        com.sun.net.httpserver.HttpServer httpServer = startServer(callback);
        server.set(httpServer);
        int port = httpServer.getAddress().getPort();
        String redirectUri = "http://127.0.0.1:" + port + "/callback";
        try {
            browse(authorizationUrl(redirectUri, state, challenge));
            Map<String, String> params = callback.get(3, TimeUnit.MINUTES);
            if (params.containsKey("error")) {
                throw new AppException("Google sign-in was cancelled.");
            }
            if (!MessageDigest.isEqual(state.getBytes(StandardCharsets.UTF_8), params.getOrDefault("state", "").getBytes(StandardCharsets.UTF_8))) {
                throw new AppException("Google sign-in could not be verified.");
            }
            String code = params.get("code");
            if (code == null || code.isBlank()) {
                throw new AppException("Google sign-in failed. Try again.");
            }
            String token = GoogleUserInfo.accessToken(exchangeCode(code, verifier, redirectUri));
            return GoogleUserInfo.verifiedEmail(loadUserInfo(token));
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AppException("Google sign-in was cancelled.", exception);
        } catch (TimeoutException exception) {
            throw new AppException("Google sign-in timed out.", exception);
        } catch (ExecutionException exception) {
            if (exception.getCause() instanceof AppException appException) {
                throw appException;
            }
            throw new AppException("Google sign-in failed. Try again.", exception.getCause());
        } finally {
            pending.set(null);
            stopServer();
        }
    }

    private com.sun.net.httpserver.HttpServer startServer(CompletableFuture<Map<String, String>> callback) {
        try {
            com.sun.net.httpserver.HttpServer httpServer = com.sun.net.httpserver.HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            httpServer.createContext("/callback", exchange -> {
                Map<String, String> params = query(exchange.getRequestURI().getRawQuery());
                String page = params.containsKey("error") ? CANCELLED_PAGE : SUCCESS_PAGE;
                byte[] body = page.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
                exchange.sendResponseHeaders(200, body.length);
                try (OutputStream output = exchange.getResponseBody()) {
                    output.write(body);
                }
                callback.complete(params);
            });
            httpServer.setExecutor(command -> {
                Thread thread = new Thread(command, "google-oauth-callback");
                thread.setDaemon(true);
                thread.start();
            });
            httpServer.start();
            return httpServer;
        } catch (IOException exception) {
            throw new AppException("Unable to wait for Google sign-in", exception);
        }
    }

    private void stopServer() {
        com.sun.net.httpserver.HttpServer current = server.getAndSet(null);
        if (current != null) {
            current.stop(0);
        }
    }

    private String authorizationUrl(String redirectUri, String state, String challenge) {
        return "https://accounts.google.com/o/oauth2/v2/auth"
                + "?client_id=" + encode(config.clientId())
                + "&redirect_uri=" + encode(redirectUri)
                + "&response_type=code"
                + "&scope=" + encode("openid email profile")
                + "&state=" + encode(state)
                + "&code_challenge=" + encode(challenge)
                + "&code_challenge_method=S256"
                + "&access_type=online"
                + "&prompt=select_account";
    }

    private String exchangeCode(String code, String verifier, String redirectUri) {
        StringBuilder form = new StringBuilder()
                .append("code=").append(encode(code))
                .append("&client_id=").append(encode(config.clientId()))
                .append("&redirect_uri=").append(encode(redirectUri))
                .append("&grant_type=authorization_code")
                .append("&code_verifier=").append(encode(verifier));
        if (config.clientSecret() != null && !config.clientSecret().isBlank()) {
            form.append("&client_secret=").append(encode(config.clientSecret()));
        }
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://oauth2.googleapis.com/token"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .timeout(Duration.ofSeconds(20))
                .POST(HttpRequest.BodyPublishers.ofString(form.toString()))
                .build();
        return send(request);
    }

    private String loadUserInfo(String accessToken) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://openidconnect.googleapis.com/v1/userinfo"))
                .header("Authorization", "Bearer " + accessToken)
                .timeout(Duration.ofSeconds(20))
                .GET()
                .build();
        return send(request);
    }

    private String send(HttpRequest request) {
        try {
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400 && !response.body().contains("\"error\"")) {
                throw new AppException("Google sign-in failed. Try again.");
            }
            return response.body();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AppException("Google sign-in was cancelled.", exception);
        } catch (IOException exception) {
            throw new AppException("Google sign-in failed. Check your connection and try again.", exception);
        }
    }

    private static void browse(String url) {
        if (!Desktop.isDesktopSupported()) {
            throw new AppException("This computer cannot open the browser for Google sign-in.");
        }
        try {
            Desktop.getDesktop().browse(URI.create(url));
        } catch (IOException exception) {
            throw new AppException("Unable to open the browser for Google sign-in.", exception);
        }
    }

    private static Map<String, String> query(String raw) {
        Map<String, String> values = new HashMap<>();
        if (raw == null || raw.isBlank()) {
            return values;
        }
        for (String pair : raw.split("&")) {
            int separator = pair.indexOf('=');
            if (separator < 0) {
                continue;
            }
            String key = URLDecoder.decode(pair.substring(0, separator), StandardCharsets.UTF_8);
            String value = URLDecoder.decode(pair.substring(separator + 1), StandardCharsets.UTF_8);
            values.put(key, value);
        }
        return values;
    }

    private static String codeChallenge(String verifier) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(verifier.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new AppException("Unable to start Google sign-in", exception);
        }
    }

    private static String randomToken() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
