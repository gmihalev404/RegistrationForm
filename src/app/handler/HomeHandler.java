package app.handler;

import app.model.User;
import app.repository.UserRepository;
import app.util.CookiesManger;
import app.util.FormDataParser;
import app.util.SessionManager;
import app.util.ViewRenderer;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class HomeHandler implements HttpHandler {
    UserRepository userRepository = new UserRepository();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();

        if (!exchange.getRequestMethod().equals("GET")) {
            ViewRenderer.renderFile(exchange, "404-not-found.html");
            return;
        }

        String sessionId = CookiesManger.getSessionIdFromCookies(exchange);

        if (!SessionManager.isLogged(sessionId)) {
            exchange.getResponseHeaders().set("Location", "/");
            exchange.sendResponseHeaders(302, -1);
            return;
        }

        Long userId = SessionManager.getUserId(sessionId).orElse(null);

        if (userId == null) {
            exchange.getResponseHeaders().set("Location", "/");
            exchange.sendResponseHeaders(302, -1);
            return;
        }

        User user = userRepository.findById(userId).orElse(null);

        if (user == null) {
            exchange.getResponseHeaders().set("Location", "/");
            exchange.sendResponseHeaders(302, -1);
            return;
        }

        ViewRenderer.renderTemplate(exchange, "home.html",
                Map.of("FIRST_NAME", user.getFirstName()));
    }
}
