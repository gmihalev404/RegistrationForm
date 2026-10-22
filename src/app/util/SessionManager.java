package app.util;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SessionManager {
    private static final Map<String, Integer> activeSessions = new ConcurrentHashMap<>();

    public static String createSession(Integer userId) {
        String sessionId = UUID.randomUUID().toString();

        if (userId != null) {
            activeSessions.put(sessionId, userId);
        }
        return sessionId;
    }

    public static Optional<Integer> getUserId(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(activeSessions.get(sessionId));
    }

    public static void invalidateSession(String sessionId) {
        if (sessionId != null) {
            activeSessions.remove(sessionId);
        }
    }

    public static boolean isLogged(String sessionId) {
        return sessionId != null && activeSessions.containsKey(sessionId);
    }
}