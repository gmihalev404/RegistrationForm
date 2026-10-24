package app;

import app.dto.LoginRequest;
import app.dto.ProfileUpdateRequest;
import app.dto.RegisterRequest;
import app.model.User;
import app.service.CaptchaService;
import app.util.CookiesManger;
import app.util.FormDataParser;
import app.util.SessionManager;
import app.validation.LoginValidator;
import app.validation.ProfileUpdateValidator;
import app.validation.RegisterValidator;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ValidationAndUtilityTest {

    @Test
    void registrationValidatesEmailPasswordAndCaptcha() {
        var challenge = CaptchaService.generateChallenge();
        RegisterRequest valid = new RegisterRequest(
                "T", "T", "test@t.com", "secret1", "secret1",
                challenge.question(), TestSupport.captchaAnswer(challenge), challenge.token());
        assertTrue(RegisterValidator.validate(valid).isEmpty());
        assertFalse(CaptchaService.verify(challenge.token(), TestSupport.captchaAnswer(challenge)));

        // Both user-specified invalid email formats.
        for (String email : List.of("test.com", "test")) {
            var captcha = CaptchaService.generateChallenge();
            RegisterRequest invalid = new RegisterRequest(
                    "T", "T", email, "secret1", "secret1",
                    captcha.question(), TestSupport.captchaAnswer(captcha), captcha.token());
            assertTrue(RegisterValidator.validate(invalid).contains("Invalid email format."));
        }

        List<String> missing = RegisterValidator.validate(
                new RegisterRequest(null, " ", null, null, null, null, null, null));
        assertTrue(missing.contains("First name cannot be empty."));
        assertTrue(missing.contains("Last name cannot be empty."));
        assertTrue(missing.contains("Password must be at least 6 characters long."));
        assertTrue(missing.contains("Please provide a CAPTCHA answer."));

        var wrongCaptcha = CaptchaService.generateChallenge();
        List<String> errors = RegisterValidator.validate(new RegisterRequest(
                "T", "T", "test@t.com", "123", "different", null,
                "999", wrongCaptcha.token()));
        assertTrue(errors.contains("Password must be at least 6 characters long."));
        assertTrue(errors.contains("Passwords do not match."));
        assertTrue(errors.contains("Incorrect CAPTCHA answer. Please try again."));
    }

    @Test
    void loginAndProfileValidation() {
        assertTrue(LoginValidator.validate(new LoginRequest("test@t.com", "secret1")).isEmpty());
        assertEquals(2, LoginValidator.validate(new LoginRequest(null, " ")).size());

        User user = new User(10L, "T", "T", "test@t.com", "oldpass");
        assertTrue(ProfileUpdateValidator.validate(
                new ProfileUpdateRequest("T", "T", null, null), user).isEmpty());
        assertTrue(ProfileUpdateValidator.validate(
                new ProfileUpdateRequest("T", "T", "oldpass", "newpass"), user).isEmpty());

        List<String> missing = ProfileUpdateValidator.validate(
                new ProfileUpdateRequest(null, " ", null, "123"), user);
        assertTrue(missing.contains("First name cannot be empty."));
        assertTrue(missing.contains("Last name cannot be empty."));
        assertTrue(missing.contains("Current password is required to change password."));
        assertTrue(missing.contains("New password must be at least 4 characters long."));

        List<String> incorrectPassword = ProfileUpdateValidator.validate(
                new ProfileUpdateRequest("T", "T", "wrong", ""), user);
        assertTrue(incorrectPassword.contains("Current password is incorrect."));
        assertTrue(incorrectPassword.contains("New password cannot be empty."));
    }

    @Test
    void formParserHandlesEncodedAndEmptyValues() {
        assertTrue(FormDataParser.parse(null).isEmpty());
        var data = FormDataParser.parse("firstName=T&email=test%40t.com&empty=&flag");
        assertEquals("T", data.get("firstName"));
        assertEquals("test@t.com", data.get("email"));
        assertEquals("", data.get("empty"));
        assertEquals("", data.get("flag"));
    }

    @Test
    void sessionAndCookieLifecycle() {
        assertTrue(SessionManager.getUserId(null).isEmpty());
        assertFalse(SessionManager.isLogged(null));

        String token = SessionManager.createSession(10L);
        assertEquals(10L, SessionManager.getUserId(token).orElseThrow());
        assertTrue(SessionManager.isLogged(token));

        var request = TestSupport.request("GET", "/", "other=1; SESSION_ID=" + token, "");
        assertEquals(token, CookiesManger.getSessionIdFromCookies(request.exchange()));
        assertNull(CookiesManger.getSessionIdFromCookies(
                TestSupport.request("GET", "/", null, "").exchange()));

        SessionManager.invalidateSession(token);
        assertFalse(SessionManager.isLogged(token));
        assertTrue(SessionManager.getUserId(token).isEmpty());
    }
}
