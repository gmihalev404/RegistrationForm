package app.handler;

import app.dto.RegisterRequest;
import app.service.CaptchaService;
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
            CaptchaService.CaptchaChallenge challenge = CaptchaService.generateChallenge();
            ViewRenderer.renderTemplate(exchange, "register.html", Map.of(
                    "ERROR_MESSAGE", "",
                    "CAPTCHA_QUESTION", challenge.question(),
                    "CAPTCHA_TOKEN", challenge.token()
            ));

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
                    parsedData.get("captchaAnswer"),
                    parsedData.get("captchaToken")
            );

            List<String> errors = registrationService.register(request);

            if (!errors.isEmpty()) {
                CaptchaService.CaptchaChallenge challenge = CaptchaService.generateChallenge();

                ViewRenderer.renderTemplate(exchange, "register.html", Map.of(
                        "ERROR_MESSAGE", errors.getFirst(),
                        "CAPTCHA_QUESTION", challenge.question(),
                        "CAPTCHA_TOKEN", challenge.token()
                ));

            } else {
                ViewRenderer.renderFile(exchange, "index.html");
            }

        } else {
            ViewRenderer.renderFile(exchange, "404-not-found.html");
        }
    }
}