package me.salman.websiteCaptcha.Website;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import me.salman.websiteCaptcha.Api.VoidWorldManager;
import me.salman.websiteCaptcha.Main;
import me.salman.websiteCaptcha.Manager.VerificationManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.UUID;

public class VerifyHandler implements HttpHandler {
    private final VerificationManager verificationManager;

    public VerifyHandler(VerificationManager verificationManager) throws IllegalArgumentException {
        if (verificationManager == null) {
            throw new IllegalArgumentException("VerificationManager cannot be null!");
        }
        this.verificationManager = verificationManager;
        Main.getInstance().getLogger().info("VerifyHandler initialized with VerificationManager: " + (verificationManager != null));
    }

    @Override
    public void handle(HttpExchange exchange) {
        try {
            if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                sendHtmlResponse(exchange, 405, "<h1>Method Not Allowed</h1>");
                return;
            }

//            Main.getInstance().getLogger().info("Processing /verify POST request at " + System.currentTimeMillis());
            String requestBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
//            Main.getInstance().getLogger().info("Received /verify POST request body: " + requestBody);

            Map<String, String> params = parseFormData(requestBody);
            String uuid = params.get("uuid");
            String recaptchaResponse = params.get("g-recaptcha-response");

            if (uuid == null || recaptchaResponse == null) {
                String errorHtml = """
                    <html>
                        <head><title>Error</title></head>
                        <body style="font-family: Arial; text-align: center; margin: 50px;">
                            <h1>Invalid Request</h1>
                            <p>UUID or CAPTCHA response is missing.</p>
                        </body>
                    </html>
                """;
//                Main.getInstance().getLogger().warning("Invalid request: uuid=" + uuid + ", g-recaptcha-response=" + recaptchaResponse);
                sendHtmlResponse(exchange, 400, errorHtml);
                return;
            }

            UUID playerUUID;
            try {
                playerUUID = UUID.fromString(uuid);
            } catch (IllegalArgumentException e) {
                String errorHtml = """
                    <html>
                        <head><title>Error</title></head>
                        <body style="font-family: Arial; text-align: center; margin: 50px;">
                            <h1>Invalid UUID</h1>
                            <p>The provided UUID is malformed.</p>
                        </body>
                    </html>
                """;
//                Main.getInstance().getLogger().warning("Invalid UUID format: " + uuid);
                sendHtmlResponse(exchange, 400, errorHtml);
                return;
            }

            if (verifyCaptcha(recaptchaResponse)) {
                if (verificationManager == null) {
                    Main.getInstance().getLogger().severe("VerificationManager is null in VerifyHandler!");
                    sendErrorResponse(exchange, 500, "Server error: Verification manager not initialized");
                    return;
                }
                verificationManager.setPlayerVerified(playerUUID, true);
//                Main.getInstance().getLogger().info("Verification succeeded for UUID: " + uuid);

                Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> {
                    Player player = Bukkit.getPlayer(playerUUID);
                    if (player != null && player.isOnline()) {
                        player.removePotionEffect(PotionEffectType.BLINDNESS);
                        if (Main.getInstance().getConfig().getBoolean("options.hide_inventory")) {
                            Main.getInstance().getInventoryManager().restoreInventory(player);
                            player.sendMessage(formatMessage(Main.getInstance().getConfig().getString("messages.inventory_restored")));
                        }
                        player.sendMessage(formatMessage(Main.getInstance().getConfig().getString("messages.verification_success")));
                        VoidWorldManager.sendToNormalWorld(player);
                        Main.getInstance().getLogger().info("Player " + playerUUID + " verified, sent to normal world.");

                        Integer taskId = verificationManager.getVerificationTask(playerUUID);
                        if (taskId != null) {
//                            Main.getInstance().getLogger().info("Cancelling task " + taskId + " for UUID " + playerUUID + " after verification.");
                            Bukkit.getScheduler().cancelTask(taskId);
                            verificationManager.removeVerificationTask(playerUUID);
                        }
                    } else {
                        Main.getInstance().getLogger().info("Player " + playerUUID + " not online after verification.");
                    }
                }, 2L);

                String successHtml = """
                    <html>
                        <head><title>Success</title></head>
                        <body style="font-family: Arial; text-align: center; margin: 50px;">
                            <h1 style="color: green;">Captcha Verified!</h1>
                            <p>You can now return to the Minecraft server.</p>
                            <button onclick="window.location.href='/';" style="padding: 10px 20px; font-size: 16px; cursor: pointer;">Return to Server</button>
                        </body>
                    </html>
                """;
                sendHtmlResponse(exchange, 200, successHtml);
            } else {
                String failureHtml = """
                    <html>
                        <head><title>Failure</title></head>
                        <body style="font-family: Arial; text-align: center; margin: 50px;">
                            <h1 style="color: red;">CAPTCHA Verification Failed</h1>
                            <p>Please try again.</p>
                            <button onclick="window.history.back();" style="padding: 10px 20px; font-size: 16px; cursor: pointer;">Retry</button>
                        </body>
                    </html>
                """;
                Main.getInstance().getLogger().info("Verification failed for UUID: " + uuid + " with error: timeout-or-duplicate");
                sendHtmlResponse(exchange, 400, failureHtml);
            }
        } catch (IOException e) {
            Main.getInstance().getLogger().severe("IOException in /verify handle: " + e.getMessage());
            e.printStackTrace();
            sendErrorResponse(exchange, 500, "Server error processing request");
        } catch (RuntimeException e) {
            Main.getInstance().getLogger().severe("RuntimeException in /verify handle: " + e.getMessage());
            e.printStackTrace();
            sendErrorResponse(exchange, 500, "Server error processing request");
        } catch (Exception e) {
            Main.getInstance().getLogger().severe("Unexpected exception in /verify handle: " + e.getMessage());
            e.printStackTrace();
            sendErrorResponse(exchange, 500, "Server error processing request");
        } finally {
            try {
                exchange.close();
            } catch (Exception e) {
                Main.getInstance().getLogger().severe("Failed to close HttpExchange: " + e.getMessage());
            }
        }
    }

    private boolean verifyCaptcha(String recaptchaResponse) {
        try {
            String secretKey = Main.getInstance().getConfig().getString("recaptcha.secret_key");
            if (secretKey == null || secretKey.isEmpty()) {
                Main.getInstance().getLogger().severe("reCAPTCHA secret key is not configured!");
                return false;
            }

            URL url = new URL("https://www.google.com/recaptcha/api/siteverify");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);

            String params = "secret=" + secretKey + "&response=" + recaptchaResponse;
            byte[] postData = params.getBytes(StandardCharsets.UTF_8);

            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setRequestProperty("Content-Length", String.valueOf(postData.length));

            try (OutputStream os = conn.getOutputStream()) {
                os.write(postData);
            }

            Scanner scanner = new Scanner(conn.getInputStream(), StandardCharsets.UTF_8.name());
            String response = scanner.useDelimiter("\\A").next();
            scanner.close();

//            Main.getInstance().getLogger().info("reCAPTCHA verification response: " + response);
            return response.contains("\"success\": true");
        } catch (Exception e) {
            Main.getInstance().getLogger().severe("Failed to verify CAPTCHA: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private Map<String, String> parseFormData(String formData) {
        Map<String, String> params = new HashMap<>();
        if (formData == null || formData.isEmpty()) {
            return params;
        }

        for (String param : formData.split("&")) {
            String[] keyValue = param.split("=", 2);
            if (keyValue.length == 2) {
                String key = URLDecoder.decode(keyValue[0], StandardCharsets.UTF_8);
                String value = URLDecoder.decode(keyValue[1], StandardCharsets.UTF_8);
                params.put(key, value);
            }
        }
        return params;
    }

    private void sendHtmlResponse(HttpExchange exchange, int statusCode, String html) {
        try {
//            Main.getInstance().getLogger().info("Attempting to send response with status " + statusCode + " at " + System.currentTimeMillis());
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            exchange.getResponseHeaders().set("Cache-Control", "no-cache, no-store, must-revalidate");
            exchange.getResponseHeaders().set("Pragma", "no-cache");
            exchange.getResponseHeaders().set("Expires", "0");

            byte[] responseBytes = html.getBytes(StandardCharsets.UTF_8);

            if (exchange.getResponseBody() == null) {
                Main.getInstance().getLogger().severe("HttpExchange response body is null, cannot send response!");
                throw new IOException("HttpExchange response body is null");
            }

            exchange.sendResponseHeaders(statusCode, responseBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(responseBytes);
                os.flush();
//                Main.getInstance().getLogger().info("Response sent with status " + statusCode + " and length " + responseBytes.length + " at " + System.currentTimeMillis());
            }
        } catch (IOException e) {
            Main.getInstance().getLogger().severe("Failed to send response: " + e.getMessage());
            e.printStackTrace();
            sendErrorResponse(exchange, 500, "Server error: Failed to send response");
        } catch (RuntimeException e) {
            Main.getInstance().getLogger().severe("Unexpected runtime error while sending response: " + e.getMessage());
            e.printStackTrace();
            sendErrorResponse(exchange, 500, "Server error: Unexpected error");
        } catch (Error e) {
            Main.getInstance().getLogger().severe("Critical error while sending response: " + e.getMessage());
            e.printStackTrace();
            sendErrorResponse(exchange, 500, "Server error: Critical error");
        }
    }

    private void sendErrorResponse(HttpExchange exchange, int statusCode, String message) {
        try {
            String errorHtml = "<html><body><h1>" + message + "</h1></body></html>";
            byte[] errorBytes = errorHtml.getBytes(StandardCharsets.UTF_8);
            if (exchange.getResponseBody() != null) {
                exchange.sendResponseHeaders(statusCode, errorBytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(errorBytes);
                    os.flush();
                }
            } else {
                Main.getInstance().getLogger().severe("Cannot send error response: HttpExchange response body is null");
            }
        } catch (IOException e) {
            Main.getInstance().getLogger().severe("Failed to send error response: " + e.getMessage());
        }
    }

    private String formatMessage(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}