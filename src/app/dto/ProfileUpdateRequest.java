package app.dto;

public record ProfileUpdateRequest(
        String firstName,
        String lastName,
        String currentPassword,
        String newPassword
) {}