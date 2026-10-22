package app.validation;

import app.dto.RegisterRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class RegisterValidator {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$");

    public static List<String> validate(RegisterRequest request) {
        List<String> errors = new ArrayList<>();

        if (request.firstName() == null || request.firstName().trim().isEmpty()) {
            errors.add("First name cannot be empty.");
        }

        if (request.lastName() == null || request.lastName().trim().isEmpty()) {
            errors.add("Last name cannot be empty.");
        }

        if (request.email() == null || !EMAIL_PATTERN.matcher(request.email()).matches()) {
            errors.add("Invalid email format.");
        }

        if (request.password() == null || request.password().length() < 6) {
            errors.add("Password must be at least 6 characters long.");
        }

        if (request.password() == null || !request.password().equals(request.confirmPassword())) {
            errors.add("Passwords do not match.");
        }

        if (request.captchaAnswer() == null || request.captchaAnswer().trim().isEmpty()) {
            errors.add("Please provide a CAPTCHA answer.");
        }

        return errors;
    }
}