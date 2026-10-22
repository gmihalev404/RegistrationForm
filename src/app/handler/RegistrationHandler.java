package app.handler;

import app.dto.RegisterRequest;
import app.service.RegistrationService;
import app.util.FormDataParser;
import app.util.ViewRenderer;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public class RegistrationHandler implements HttpHandler {
    private final RegistrationService registrationService = new RegistrationService();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();

        if (method.equals("GET")) {
            // При отваряне на страницата подаваме празно съобщение за грешка
            ViewRenderer.renderTemplate(exchange, "register.html", Map.of("ERROR_MESSAGE", ""));

        } else if (method.equals("POST")) {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            Map<String, String> parsedData = FormDataParser.parse(body);

            RegisterRequest request = new RegisterRequest(
                    parsedData.get("firstName"),
                    parsedData.get("lastName"),
                    parsedData.get("email"),
                    parsedData.get("password"),
                    parsedData.get("confirmPassword"),
                    parsedData.get("captchaQuestion"),
                    parsedData.get("captchaAnswer")
            );

            List<String> errors = registrationService.register(request);

            if (!errors.isEmpty()) {
                ViewRenderer.renderTemplate(exchange, "register.html", Map.of("ERROR_MESSAGE", errors.getFirst()));
            } else {
                ViewRenderer.renderFile(exchange, "index.html");
            }

        } else {
            ViewRenderer.renderFile(exchange, "404-not-found.html");
        }
    }
}