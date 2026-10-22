package app.validation;

import app.dto.LoginRequest;

import java.util.ArrayList;
import java.util.List;

public class LoginValidator {

    public static List<String> validate(LoginRequest request) {
        List<String> errors = new ArrayList<>();

        if (request.email() == null || request.email().isBlank()) {
            errors.add("Email cannot be empty.");
        }

        if (request.password() == null || request.password().isBlank()) {
            errors.add("Password cannot be empty.");
        }

        return errors;
    }
}
