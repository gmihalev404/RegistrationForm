package app.handler;

import app.util.CookiesManger;
import app.util.SessionManager;
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
            String sessionId = CookiesManger.getSessionIdFromCookies(exchange);

            if (SessionManager.isLogged(sessionId)) {
                exchange.getResponseHeaders().set("Location", "/home");
                exchange.sendResponseHeaders(302, -1);
                return;
            }

            ViewRenderer.renderFile(exchange, "index.html");
            return;
        }

        ViewRenderer.renderFile(exchange, "404-not-found.html");
    }
}