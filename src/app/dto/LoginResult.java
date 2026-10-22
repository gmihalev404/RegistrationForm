package app.dto;

import java.util.List;

public record LoginResult(
        List<String> errors,
        String sessionId
) {
    public boolean isSuccessful() {
        return errors.isEmpty();
    }
}