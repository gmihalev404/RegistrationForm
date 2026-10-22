package app.util;

import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class ViewRenderer {

    public static void renderFile(HttpExchange exchange, String relativePath) throws IOException {
        renderTemplate(exchange, relativePath, Map.of("ERROR_MESSAGE", ""));
    }

    public static void renderTemplate(HttpExchange exchange, String relativePath, Map<String, String> variables) throws IOException {
        if (relativePath.startsWith("/")) {
            relativePath = relativePath.substring(1);
        }

        Path filePath = Path.of("src", "web", relativePath);
        String content;
        int code = 200;

        try {
            if (Files.exists(filePath) && !Files.isDirectory(filePath)) {
                content = Files.readString(filePath, StandardCharsets.UTF_8);
            } else {
                content = "File Not Found";
                code = 404;
            }
        } catch (IOException e) {
            content = "Server Error: " + e.getMessage();
            code = 500;
        }

        for (Map.Entry<String, String> entry : variables.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            if ("ERROR_MESSAGE".equals(key)) {
                if (value != null && !value.isBlank()) {
                    String alertHtml = "<div class=\"alert alert-danger p-2 mb-3 small rounded-3 text-center\" role=\"alert\">"
                            + value + "</div>";
                    content = content.replace("{{" + key + "}}", alertHtml);
                } else {
                    content = content.replace("{{" + key + "}}", "");
                }
            } else {
                content = content.replace("{{" + key + "}}", value != null ? value : "");
            }
        }

        byte[] response = content.getBytes(StandardCharsets.UTF_8);

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