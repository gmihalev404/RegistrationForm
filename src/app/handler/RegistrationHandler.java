package app.handler;

import app.dto.RegisterRequest;
import app.util.FormDataParser;
import app.util.ViewRenderer;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class RegistrationHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();

        if (method.equals("GET")) {
            ViewRenderer.renderFile(exchange, "register.html");

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

            System.out.println("Received Registration Request:");
            System.out.println("Email: " + request.email());
            System.out.println("First Name: " + request.firstName());
            System.out.println("Last Name: " + request.lastName());
            System.out.println("CAPTCHA Answer: " + request.captchaAnswer());

            ViewRenderer.renderFile(exchange, "index.html");

        } else {
            ViewRenderer.renderFile(exchange, "404-not-found.html");
        }
    }
}