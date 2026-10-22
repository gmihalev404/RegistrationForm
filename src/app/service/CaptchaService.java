package app.service;

import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CaptchaService {
    private static final Map<String, Integer> captchaStore = new ConcurrentHashMap<>();
    private static final Random random = new Random();

    public record CaptchaChallenge(String token, String question) {}

    public static CaptchaChallenge generateChallenge() {
        int num1 = random.nextInt(20) + 1;
        int num2 = random.nextInt(20) + 1;
        int answer = num1 + num2;

        String token = UUID.randomUUID().toString();
        String question = num1 + " + " + num2;

        captchaStore.put(token, answer);
        return new CaptchaChallenge(token, question);
    }

    public static boolean verify(String token, String userAnswerStr) {
        if (token == null || !captchaStore.containsKey(token)) {
            return false;
        }

        try {
            int userAnswer = Integer.parseInt(userAnswerStr.trim());
            int correctAnswer = captchaStore.get(token);

            captchaStore.remove(token);

            return userAnswer == correctAnswer;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}