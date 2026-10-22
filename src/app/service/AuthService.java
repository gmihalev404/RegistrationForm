package app.service;

import app.dto.LoginRequest;
import app.dto.LoginResult;
import app.model.User;
import app.repository.UserRepository;
import app.validation.LoginValidator;
import app.util.SessionManager;

import java.util.List;

public class AuthService {

    private final UserRepository userRepository = new UserRepository();

    public LoginResult login(LoginRequest request) {
        List<String> validationErrors = LoginValidator.validate(request);

        if (!validationErrors.isEmpty()) {
            return new LoginResult(validationErrors, null);
        }

        User user = userRepository.findByEmailAndPassword(request.email(), request.password()).orElse(null);

        if (user == null) {
            return new LoginResult(List.of("Invalid email or password."), null);
        }

        String sessionId = SessionManager.createSession(user.getId());

        return new LoginResult(List.of(), sessionId);
    }

    public void logout(String sessionId) {
        if (sessionId != null && !sessionId.isBlank()) {
            SessionManager.invalidateSession(sessionId);
        }
    }
}