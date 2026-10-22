package app.handler;

import app.util.CookiesManger;
import app.util.SessionManager;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;

public class LogoutHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String sessionId = CookiesManger.getSessionIdFromCookies(exchange);

        if (sessionId != null) {
            SessionManager.invalidateSession(sessionId);
        }

        exchange.getResponseHeaders().set("Location", "/");
        exchange.sendResponseHeaders(302, -1);
    }
}
