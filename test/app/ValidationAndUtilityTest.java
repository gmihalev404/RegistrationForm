package app;

import app.dto.*;
import app.model.User;
import app.service.CaptchaService;
import app.util.*;
import app.validation.*;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ValidationAndUtilityTest {

    @Test
    void registrationAcceptsValidCaptchaAndRejectsBadInput() {
        CaptchaService.CaptchaChallenge challenge = CaptchaService.generateChallenge();
        RegisterRequest valid = new RegisterRequest(
                "T", "T", "test@t.com", "secret1", "secret1",
                challenge.question(), TestSupport.captchaAnswer(challenge), challenge.token());
        assertTrue(RegisterValidator.validate(valid).isEmpty());
        assertFalse(CaptchaService.verify(challenge.token(), TestSupport.captchaAnswer(challenge)),
                "A solved CAPTCHA is single use");

        List<String> missing = RegisterValidator.validate(
                new RegisterRequest(null, " ", null, null, null, null, null, null));
        assertTrue(missing.contains("First name cannot be empty."));
        assertTrue(missing.contains("Last name cannot be empty."));
        assertTrue(missing.contains("Invalid email format."));
        assertTrue(missing.contains("Password must be at least 6 characters long."));
        assertTrue(missing.contains("Passwords do not match."));
        assertTrue(missing.contains("Please provide a CAPTCHA answer."));

        CaptchaService.CaptchaChallenge wrong = CaptchaService.generateChallenge();
        RegisterRequest invalid = new RegisterRequest(" ", "", "test.com", "123",
                "different", null, "999", wrong.token());
        List<String> errors = RegisterValidator.validate(invalid);
        assertTrue(errors.contains("Incorrect CAPTCHA answer. Please try again."));
        assertTrue(errors.contains("Passwords do not match."));
        assertTrue(errors.contains("Invalid email format."));
        assertTrue(errors.contains("Password must be at least 6 characters long."));

        CaptchaService.CaptchaChallenge malformedEmailCaptcha = CaptchaService.generateChallenge();
        RegisterRequest malformedEmail = new RegisterRequest("T", "T", "test",
                "secret1", "secret1", malformedEmailCaptcha.question(),
                TestSupport.captchaAnswer(malformedEmailCaptcha), malformedEmailCaptcha.token());
        assertTrue(RegisterValidator.validate(malformedEmail).contains("Invalid email format."));

    }

    @Test
    void captchaRejectsUnknownWrongAndNonNumericAnswers() {
        assertFalse(CaptchaService.verify(null, "3"));
        assertFalse(CaptchaService.verify("missing-token", "3"));

        CaptchaService.CaptchaChallenge challenge = CaptchaService.generateChallenge();
        assertFalse(CaptchaService.verify(challenge.token(), "not-a-number"));
        assertTrue(CaptchaService.verify(challenge.token(), " " +
                TestSupport.captchaAnswer(challenge) + " "));
        assertFalse(CaptchaService.verify(challenge.token(), "1"));

        CaptchaService.CaptchaChallenge other = CaptchaService.generateChallenge();
        int answer = Integer.parseInt(TestSupport.captchaAnswer(other));
        assertFalse(CaptchaService.verify(other.token(), String.valueOf(answer + 1)));
        assertFalse(CaptchaService.verify(other.token(), String.valueOf(answer)));
    }

    @Test
    void loginAndProfileValidationCoverSuccessfulAndFailedChanges() {
        assertTrue(LoginValidator.validate(new LoginRequest("test@t.com", "pwd")).isEmpty());
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

        List<String> wrong = ProfileUpdateValidator.validate(
                new ProfileUpdateRequest("T", "T", "incorrect", ""), user);
        assertTrue(wrong.contains("Current password is incorrect."));
        assertTrue(wrong.contains("New password cannot be empty."));
    }

    @Test
    void formParserHandlesEncodingEmptyValuesAndMissingBody() {
        assertTrue(FormDataParser.parse(null).isEmpty());
        assertTrue(FormDataParser.parse("  ").isEmpty());
        Map<String, String> parsed = FormDataParser.parse(
                "name=T+T&city=Sofia%20BG&empty=&flag&skip=x=y");
        assertEquals("T T", parsed.get("name"));
        assertEquals("Sofia BG", parsed.get("city"));
        assertEquals("", parsed.get("empty"));
        assertEquals("", parsed.get("flag"));
        assertFalse(parsed.containsKey("skip"));
    }

    @Test
    void sessionsAndCookieParsingCoverLoginAndLogout() {
        assertTrue(SessionManager.getUserId(null).isEmpty());
        assertTrue(SessionManager.getUserId(" ").isEmpty());
        assertFalse(SessionManager.isLogged(null));
        String token = SessionManager.createSession(10L);
        assertEquals(10L, SessionManager.getUserId(token).orElseThrow());
        assertTrue(SessionManager.isLogged(token));

        var exchange = TestSupport.request("GET", "/", "theme=light; SESSION_ID=" + token, "");
        assertEquals(token, CookiesManger.getSessionIdFromCookies(exchange.exchange()));
        assertNull(CookiesManger.getSessionIdFromCookies(
                TestSupport.request("GET", "/", null, "").exchange()));
        assertNull(CookiesManger.getSessionIdFromCookies(
                TestSupport.request("GET", "/", "other=one; invalid-cookie", "").exchange()));

        SessionManager.invalidateSession(token);
        SessionManager.invalidateSession(null);
        assertFalse(SessionManager.isLogged(token));
        assertTrue(SessionManager.getUserId(token).isEmpty());
        String noUserToken = SessionManager.createSession(null);
        assertFalse(SessionManager.isLogged(noUserToken));
    }

    @Test
    void userAndResultObjectsExposeCorrectState() {
        User user = new User(null, null, "test@t.com", "password");
        assertNull(user.getId());
        assertEquals("test@t.com", user.getEmail());
        user.setFirstName("T");
        user.setLastName("T");
        user.setPassword("changed");
        assertEquals("T", user.getFirstName());
        assertEquals("T", user.getLastName());
        assertEquals("changed", user.getPassword());

        assertTrue(new RegistrationResult(List.of(), "session").isSuccessful());
        assertFalse(new RegistrationResult(List.of("error"), null).isSuccessful());
        assertTrue(new LoginResult(List.of(), "session").isSuccessful());
        assertFalse(new LoginResult(List.of("error"), null).isSuccessful());
        assertTrue(new ProfileUpdateResult(List.of(), true).isSuccessful());
        assertFalse(new ProfileUpdateResult(List.of(), false).isSuccessful());
        assertFalse(new ProfileUpdateResult(List.of("error"), true).isSuccessful());
    }
}
