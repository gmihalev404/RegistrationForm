package app.dto;

public record LoginRequest (
    String email,
    String password
    ) {}
