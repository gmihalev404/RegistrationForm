package app.service;

import app.dto.RegisterRequest;
import app.dto.RegistrationResult;
import app.model.User;
import app.repository.UserRepository;
import app.util.SessionManager;
import app.validation.RegisterValidator;

import java.util.List;
import java.util.Optional;
import java.util.Random;

public class RegistrationService {

    private final UserRepository userRepository = new UserRepository();

    public RegistrationResult register(RegisterRequest request) {
        List<String> validationErrors = RegisterValidator.validate(request);

        if (!validationErrors.isEmpty()) {
            return new RegistrationResult(validationErrors, null);
        }

        Optional<User> existingUser = userRepository.findByEmail(request.email());
        if (existingUser.isPresent()) {
            return new RegistrationResult(
                    List.of("An account with this email already exists.")
                    , null);
        }

        User newUser = new User(
                request.firstName(),
                request.lastName(),
                request.email(),
                request.password()
        );

        userRepository.save(newUser);

        User user = userRepository.findByEmailAndPassword(newUser.getEmail(), newUser.getPassword()).orElse(null);

        String sessionId = SessionManager.createSession(user.getId());

        return new RegistrationResult(List.of(), sessionId);
    }
}