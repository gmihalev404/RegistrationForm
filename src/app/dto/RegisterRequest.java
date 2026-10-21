package app.dto;

public record RegisterRequest(
        String firstName,
        String lastName,
        String email,
        String password,
        String confirmPassword,
        String captchaQuestion,
        String captchaAnswer
) {
}