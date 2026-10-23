package app.handler;

import app.service.AuthService;
import app.util.CookiesManger;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;

public class LogoutHandler implements HttpHandler {
    private final AuthService authService = new AuthService();
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String sessionId = CookiesManger.getSessionIdFromCookies(exchange);

        authService.logout(sessionId);

        exchange.getResponseHeaders().set("Location", "/");
        exchange.sendResponseHeaders(302, -1);
    }
}
