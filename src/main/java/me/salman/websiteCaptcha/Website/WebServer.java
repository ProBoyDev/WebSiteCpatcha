package me.salman.websiteCaptcha.Website;

import com.sun.net.httpserver.HttpServer;
import me.salman.websiteCaptcha.Main;
import me.salman.websiteCaptcha.Manager.VerificationManager;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public class WebServer {
    private final HttpServer server;

    public WebServer(VerificationManager verificationManager) throws IllegalArgumentException, IOException {
        if (verificationManager == null) {
            throw new IllegalArgumentException("VerificationManager cannot be null!");
        }

        int port = Main.getInstance().getConfig().getInt("Web.port", 8080);
        String host = Main.getInstance().getConfig().getString("Web.host", "localhost");
        server = HttpServer.create(new InetSocketAddress(host, port), 0);

        server.createContext("/captcha", new CaptchaHandler());
        server.createContext("/verify", new VerifyHandler(verificationManager));
        Main.getInstance().getLogger().info("WebServer initialized on " + host + ":" + port + " with VerificationManager: " + (verificationManager != null));

        server.setExecutor(Executors.newFixedThreadPool(10));
    }

    public void start() {
        server.start();
        Main.getInstance().getLogger().info("WebServer started on " + server.getAddress());
    }

    public void stop() {
        server.stop(0);
        Main.getInstance().getLogger().info("WebServer stopped.");
    }
}