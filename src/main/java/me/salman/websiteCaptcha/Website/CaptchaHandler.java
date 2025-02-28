package me.salman.websiteCaptcha.Website;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import me.salman.websiteCaptcha.Main;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class CaptchaHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String siteKey = Main.getInstance().getConfig().getString("recaptcha.site_key");
        if (siteKey == null || siteKey.isEmpty()) {
            String errorMessage = "reCAPTCHA site key is not configured!";
            Main.getInstance().getLogger().severe(errorMessage);
            sendResponse(exchange, 500, errorMessage);
            return;
        }

        String query = exchange.getRequestURI().getQuery();
        if (query == null || !query.contains("uuid=")) {
            String errorMessage = "Missing UUID in the query!";
            Main.getInstance().getLogger().warning("Invalid /captcha request: " + query);
            sendResponse(exchange, 400, errorMessage);
            return;
        }

        String uuid = query.split("uuid=")[1];
        String cacheBust = String.valueOf(System.currentTimeMillis()); // Prevent caching
        String response = """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>Google reCAPTCHA</title>
                    <script src="https://www.google.com/recaptcha/api.js?cb=%s" async defer></script>
                    <style>
                        body {
                            font-family: Arial, sans-serif;
                            background-color: #f4f4f9;
                            display: flex;
                            justify-content: center;
                            align-items: center;
                            height: 100vh;
                            margin: 0;
                        }
                        .container {
                            text-align: center;
                            background: #ffffff;
                            border-radius: 10px;
                            box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
                            padding: 20px;
                            width: 300px;
                        }
                        h1 {
                            color: #333;
                        }
                        button {
                            background-color: #4CAF50;
                            color: white;
                            border: none;
                            padding: 10px 20px;
                            text-align: center;
                            font-size: 16px;
                            border-radius: 5px;
                            cursor: pointer;
                        }
                        button:hover {
                            background-color: #45a049;
                        }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <h1>Verify Yourself</h1>
                        <p>Please complete the CAPTCHA below to proceed.</p>
                        <form action="/verify" method="POST">
                            <input type="hidden" name="uuid" value="%s" />
                            <div class="g-recaptcha" data-sitekey="%s"></div>
                            <br />
                            <button type="submit">Verify</button>
                        </form>
                    </div>
                </body>
                </html>
                """.formatted(cacheBust, uuid, siteKey);

        sendResponse(exchange, 200, response);
    }

    private void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
        exchange.getResponseHeaders().set("Cache-Control", "no-store, no-cache, must-revalidate, private");
        exchange.getResponseHeaders().set("Pragma", "no-cache");
        exchange.getResponseHeaders().set("Expires", "0");
        byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, responseBytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }
    }
}