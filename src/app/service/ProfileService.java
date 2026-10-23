package app.service;

import app.dto.ProfileUpdateRequest;
import app.dto.ProfileUpdateResult;
import app.model.User;
import app.repository.UserRepository;
import app.validation.ProfileUpdateValidator;

import java.util.List;

public class ProfileService {
    private final UserRepository userRepository = new UserRepository();

    public ProfileUpdateResult updateProfile(Long userId, ProfileUpdateRequest request) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return new ProfileUpdateResult(List.of("User not found."), false);
        }

        List<String> validationErrors = ProfileUpdateValidator.validate(request, user);
        if (!validationErrors.isEmpty()) {
            return new ProfileUpdateResult(validationErrors, false);
        }

        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());

        if (request.newPassword() != null && !request.newPassword().isBlank()) {
            user.setPassword(request.newPassword());
        }

        userRepository.save(user);

        return new ProfileUpdateResult(List.of(), true);
    }
}