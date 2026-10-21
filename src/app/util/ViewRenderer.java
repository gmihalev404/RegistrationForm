package app.util;

import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class ViewRenderer {

    public static void renderFile(HttpExchange exchange, String relativePath) throws IOException {
        if (relativePath.startsWith("/")) {
            relativePath = relativePath.substring(1);
        }

        Path filePath = Path.of("src", "web", relativePath);
        byte[] response;
        int code = 200;

        try {
            if (Files.exists(filePath) && !Files.isDirectory(filePath)) {
                response = Files.readAllBytes(filePath);
            } else {
                response = "File Not Found".getBytes(StandardCharsets.UTF_8);
                code = 404;
            }
        } catch (IOException e) {
            response = ("Server Error: " + e.getMessage()).getBytes(StandardCharsets.UTF_8);
            code = 500;
        }

        String contentType = "text/plain; charset=UTF-8";

        if (relativePath.endsWith(".html")) {
            contentType = "text/html; charset=UTF-8";
        } else if (relativePath.endsWith(".css")) {
            contentType = "text/css; charset=UTF-8";
        }

        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(code, response.length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response);
        }
    }
}