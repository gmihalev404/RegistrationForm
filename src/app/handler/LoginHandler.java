package app.handler;

import app.dto.LoginRequest;
import app.dto.LoginResult;
import app.service.AuthService;
import app.util.FormDataParser;
import app.util.ViewRenderer;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class LoginHandler implements HttpHandler {
    private final AuthService authService = new AuthService();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();

        if (method.equals("GET")) {
            ViewRenderer.renderTemplate(exchange, "login.html", Map.of(
                    "ERROR_MESSAGE", ""
            ));

        } else if (method.equals("POST")) {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            Map<String, String> parsedData = FormDataParser.parse(body);

            LoginRequest request = new LoginRequest(
                    parsedData.get("email"),
                    parsedData.get("password")
            );

            LoginResult result = authService.login(request);

            if (!result.isSuccessful()) {
                ViewRenderer.renderTemplate(exchange, "login.html", Map.of(
                        "ERROR_MESSAGE", result.errors().getFirst()
                ));

            } else {
                exchange.getResponseHeaders().set("Set-Cookie", "SESSION_ID=" + result.sessionId() + "; Path=/; HttpOnly");
                exchange.getResponseHeaders().set("Location", "/home");
                exchange.sendResponseHeaders(302, -1);
            }

        } else {
            ViewRenderer.renderFile(exchange, "404-not-found.html");
        }
    }
}