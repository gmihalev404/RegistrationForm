package app.dto;

import java.util.List;

public record ProfileUpdateResult(
        List<String> errors,
        boolean valid
) {
    public boolean isSuccessful() {
        return errors.isEmpty() && valid;
    }
}