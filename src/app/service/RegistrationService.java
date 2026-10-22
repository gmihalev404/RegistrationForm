package app.service;

import app.dto.RegisterRequest;
import app.validation.RegisterValidator;

import java.util.List;

public class RegistrationService {

    public List<String> register(RegisterRequest request) {
        List<String> validationErrors = RegisterValidator.validate(request);

        if (!validationErrors.isEmpty()) {
            return validationErrors;
        }

        return List.of();
    }
}