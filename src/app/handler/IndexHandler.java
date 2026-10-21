package app.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class IndexHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();

        if (path.endsWith(".css")) {
            try {
                Path cssPath = Path.of("src", "web", path);
                byte[] cssResponse = Files.readAllBytes(cssPath);

                exchange.getResponseHeaders().set("Content-Type", "text/css; charset=UTF-8");
                exchange.sendResponseHeaders(200, cssResponse.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(cssResponse);
                }
            } catch (IOException e) {
                String err = "CSS Not Found";
                exchange.sendResponseHeaders(404, err.length());
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(err.getBytes());
                }
            }
            return;
        }

        if (!path.equals("/")) {
            String response = "404 (Not Found)";
            exchange.sendResponseHeaders(404, response.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
            return;
        }

        String method = exchange.getRequestMethod();
        int code = 200;
        byte[] response;

        if (!method.equals("GET")) {
            response = "Method Not Allowed".getBytes(StandardCharsets.UTF_8);
            code = 405;
        } else {
            try {
                Path indexPath = Path.of("src", "web", "index.html");
                indexPath = Path.of("src", "web", "login.html");
                //indexPath = Path.of("src", "web", "register.html");
                //indexPath = Path.of("src", "web", "404-not-found.html");

                response = Files.readAllBytes(indexPath);
            } catch (IOException e) {
                System.out.println("File read error: " + e.getMessage());
                response = "File Not Found".getBytes(StandardCharsets.UTF_8);
                code = 404;
            }
        }

        if (code == 200) {
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
        } else {
            exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
        }

        exchange.sendResponseHeaders(code, response.length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response);
        } catch (IOException e) {
            System.out.println(e.getMessage() + ": " + e.getCause());
        }
    }
}