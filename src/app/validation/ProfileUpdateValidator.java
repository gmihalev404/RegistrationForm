package app.validation;

import app.dto.ProfileUpdateRequest;
import app.model.User;

import java.util.ArrayList;
import java.util.List;

public class ProfileUpdateValidator {

    public static List<String> validate(ProfileUpdateRequest request, User existingUser) {
        List<String> errors = new ArrayList<>();

        if (request.firstName() == null || request.firstName().isBlank()) {
            errors.add("First name cannot be empty.");
        }

        if (request.lastName() == null || request.lastName().isBlank()) {
            errors.add("Last name cannot be empty.");
        }

        boolean isAttemptingPasswordChange =
                (request.currentPassword() != null && !request.currentPassword().isBlank()) ||
                        (request.newPassword() != null && !request.newPassword().isBlank());

        if (isAttemptingPasswordChange) {
            if (request.currentPassword() == null || request.currentPassword().isBlank()) {
                errors.add("Current password is required to change password.");
            } else if (!request.currentPassword().equals(existingUser.getPassword())) {
                errors.add("Current password is incorrect.");
            }

            if (request.newPassword() == null || request.newPassword().isBlank()) {
                errors.add("New password cannot be empty.");
            } else if (request.newPassword().length() < 4) { // Може да настроите минимална дължина по избор
                errors.add("New password must be at least 4 characters long.");
            }
        }

        return errors;
    }
}