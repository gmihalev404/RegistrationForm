package app.handler;

import app.util.ViewRenderer;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;

public class IndexHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();

        if (!exchange.getRequestMethod().equals("GET")) {
            ViewRenderer.renderFile(exchange, "404-not-found.html");
            return;
        }

        if (path.endsWith(".css")) {
            ViewRenderer.renderFile(exchange, path);
            return;
        }

        if (path.equals("/")) {
//            ViewRenderer.renderFile(exchange, "register.html");
            // ViewRenderer.renderFile(exchange, "login.html");
             ViewRenderer.renderFile(exchange, "index.html");
            return;
        }

        ViewRenderer.renderFile(exchange, "404-not-found.html");
    }
}