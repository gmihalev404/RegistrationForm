package app.handler;

import app.dto.ProfileUpdateRequest;
import app.dto.ProfileUpdateResult;
import app.model.User;
import app.repository.UserRepository;
import app.service.ProfileService;
import app.util.CookiesManger;
import app.util.FormDataParser;
import app.util.SessionManager;
import app.util.ViewRenderer;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

public class ProfileHandler implements HttpHandler {
    private final UserRepository userRepository = new UserRepository();
    private final ProfileService profileService = new ProfileService();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String sessionId = CookiesManger.getSessionIdFromCookies(exchange);

        if (!SessionManager.isLogged(sessionId)) {
            redirectToLogin(exchange);
            return;
        }

        Long userId = SessionManager.getUserId(sessionId).orElse(null);
        if (userId == null) {
            redirectToLogin(exchange);
            return;
        }

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            redirectToLogin(exchange);
            return;
        }

        if (method.equals("GET")) {
            ViewRenderer.renderTemplate(exchange, "profile.html", Map.of(
                    "ERROR_MESSAGE", "",
                    "SUCCESS_MESSAGE", "",
                    "FIRST_NAME", user.getFirstName() != null ? user.getFirstName() : "",
                    "LAST_NAME", user.getLastName() != null ? user.getLastName() : ""
            ));        }

        else if (method.equals("POST")) {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            Map<String, String> parsedData = FormDataParser.parse(body);

            ProfileUpdateRequest request = new ProfileUpdateRequest(
                    parsedData.get("firstName"),
                    parsedData.get("lastName"),
                    parsedData.get("currentPassword"),
                    parsedData.get("newPassword")
            );

            ProfileUpdateResult result = profileService.updateProfile(userId, request);

            if (!result.isSuccessful()) {
                ViewRenderer.renderTemplate(exchange, "profile.html", Map.of(
                        "ERROR_MESSAGE", result.errors().getFirst(),
                        "SUCCESS_MESSAGE", "",
                        "FIRST_NAME", request.firstName() != null ? request.firstName() : "",
                        "LAST_NAME", request.lastName() != null ? request.lastName() : ""
                ));
            } else {
                User updatedUser = userRepository.findById(userId).get();
                ViewRenderer.renderTemplate(exchange, "profile.html", Map.of(
                        "ERROR_MESSAGE", "",
                        "SUCCESS_MESSAGE", "Profile updated successfully!",
                        "FIRST_NAME", updatedUser.getFirstName() != null ? updatedUser.getFirstName() : "",
                        "LAST_NAME", updatedUser.getLastName() != null ? updatedUser.getLastName() : ""
                ));            }
        }
        else {
            ViewRenderer.renderFile(exchange, "404-not-found.html");
        }
    }

    private void redirectToLogin(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().set("Location", "/login");
        exchange.sendResponseHeaders(302, -1);
    }
}