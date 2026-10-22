package app.service;

import app.dto.RegisterRequest;
import app.model.User;
import app.repository.UserRepository;
import app.validation.RegisterValidator;

import java.util.List;
import java.util.Optional;

public class RegistrationService {

    private final UserRepository userRepository = new UserRepository();

    public List<String> register(RegisterRequest request) {
        List<String> validationErrors = RegisterValidator.validate(request);

        if (!validationErrors.isEmpty()) {
            return validationErrors;
        }

        Optional<User> existingUser = userRepository.findByEmail(request.email());
        if (existingUser.isPresent()) {
            return List.of("An account with this email already exists.");
        }

        User newUser = new User(
                request.firstName(),
                request.lastName(),
                request.email(),
                request.password()
        );

        userRepository.save(newUser);

        return List.of();
    }
}