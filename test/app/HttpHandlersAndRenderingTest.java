package app;

import app.dto.*;
import app.handler.*;
import app.model.User;
import app.repository.UserRepository;
import app.service.AuthService;
import app.service.ProfileService;
import app.service.RegistrationService;
import app.util.SessionManager;
import app.util.ViewRenderer;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class HttpHandlersAndRenderingTest {

    @Test
    void indexRendersPagesAndRedirectsLoggedInUser() throws Exception {
        IndexHandler handler = new IndexHandler();

        var anonymous = TestSupport.request("GET", "/", null, "");
        handler.handle(anonymous.exchange());
        assertTrue(anonymous.content().contains("Welcome"));
        verify(anonymous.exchange()).sendResponseHeaders(eq(200), anyLong());

        var css = TestSupport.request("GET", "/css/app.css", null, "");
        handler.handle(css.exchange());
        assertTrue(css.headers().getFirst("Content-Type").startsWith("text/css"));

        var missing = TestSupport.request("GET", "/unknown", null, "");
        handler.handle(missing.exchange());
        assertTrue(missing.content().contains("404"));

        var wrongMethod = TestSupport.request("POST", "/", null, "");
        handler.handle(wrongMethod.exchange());
        assertTrue(wrongMethod.content().contains("404"));

        String session = SessionManager.createSession(1L);
        try {
            var loggedIn = TestSupport.request("GET", "/", "SESSION_ID=" + session, "");
            handler.handle(loggedIn.exchange());
            assertEquals("/home", loggedIn.headers().getFirst("Location"));
            verify(loggedIn.exchange()).sendResponseHeaders(302, -1);
        } finally {
            SessionManager.invalidateSession(session);
        }
    }

    @Test
    void loginHandlesGetBadCredentialsSuccessAndUnsupportedMethod() throws Exception {
        AuthService auth = mock(AuthService.class);
        LoginHandler handler = new LoginHandler();
        TestSupport.inject(handler, "authService", auth);

        var get = TestSupport.request("GET", "/login", null, "");
        handler.handle(get.exchange());
        assertTrue(get.content().contains("Sign In"));

        when(auth.login(any(LoginRequest.class)))
                .thenReturn(new LoginResult(List.of("Invalid email or password."), null))
                .thenReturn(new LoginResult(List.of(), "a-session"));

        var invalid = TestSupport.request("POST", "/login", null,
                "email=test%40t.com&password=wrong");
        handler.handle(invalid.exchange());
        assertTrue(invalid.content().contains("Invalid email or password."));

        var valid = TestSupport.request("POST", "/login", null,
                "email=test%40t.com&password=secret1");
        handler.handle(valid.exchange());
        assertEquals("/home", valid.headers().getFirst("Location"));
        assertTrue(valid.headers().getFirst("Set-Cookie").contains("SESSION_ID=a-session"));
        verify(valid.exchange()).sendResponseHeaders(302, -1);
        verify(auth, times(2)).login(any(LoginRequest.class));

        var other = TestSupport.request("DELETE", "/login", null, "");
        handler.handle(other.exchange());
        assertTrue(other.content().contains("404"));
    }

    @Test
    void registrationHandlesFormFailedSubmissionAndSuccessfulSubmission() throws Exception {
        RegistrationService registration = mock(RegistrationService.class);
        RegistrationHandler handler = new RegistrationHandler();
        TestSupport.inject(handler, "registrationService", registration);

        var get = TestSupport.request("GET", "/register", null, "");
        handler.handle(get.exchange());
        assertTrue(get.content().contains("Create Account"));
        assertFalse(get.content().contains("{{CAPTCHA_TOKEN}}"));

        when(registration.register(any(RegisterRequest.class)))
                .thenReturn(new RegistrationResult(List.of("Invalid email format."), null))
                .thenReturn(new RegistrationResult(List.of(), "new-session"));

        String body = "firstName=T&lastName=T&email=test%40t.com" +
                "&password=secret1&confirmPassword=secret1&captchaAnswer=4&captchaToken=token";
        var invalid = TestSupport.request("POST", "/register", null,
                body.replace("test%40t.com", "test.com"));
        handler.handle(invalid.exchange());
        assertTrue(invalid.content().contains("Invalid email format."));
        assertFalse(invalid.content().contains("{{CAPTCHA_TOKEN}}"));

        var valid = TestSupport.request("POST", "/register", null, body);
        handler.handle(valid.exchange());
        assertEquals("/home", valid.headers().getFirst("Location"));
        assertTrue(valid.headers().getFirst("Set-Cookie").contains("SESSION_ID=new-session"));
        verify(valid.exchange()).sendResponseHeaders(302, -1);

        var other = TestSupport.request("PUT", "/register", null, "");
        handler.handle(other.exchange());
        assertTrue(other.content().contains("404"));
    }

    @Test
    void homeHandlesUnauthorizedMissingAndExistingUsers() throws Exception {
        HomeHandler handler = new HomeHandler();
        UserRepository repo = mock(UserRepository.class);
        TestSupport.inject(handler, "userRepository", repo);

        var notLogged = TestSupport.request("GET", "/home", null, "");
        handler.handle(notLogged.exchange());
        assertEquals("/", notLogged.headers().getFirst("Location"));

        String session = SessionManager.createSession(7L);
        try {
            var notFound = TestSupport.request("GET", "/home", "SESSION_ID=" + session, "");
            when(repo.findById(7L)).thenReturn(Optional.empty());
            handler.handle(notFound.exchange());
            assertEquals("/", notFound.headers().getFirst("Location"));

            when(repo.findById(7L))
                    .thenReturn(Optional.of(new User(7L, "T", "T", "test@t.com", "secret")));
            var success = TestSupport.request("GET", "/home", "SESSION_ID=" + session, "");
            handler.handle(success.exchange());
            assertTrue(success.content().contains("Welcome, T"));

            var unsupported = TestSupport.request("PUT", "/home", "SESSION_ID=" + session, "");
            handler.handle(unsupported.exchange());
            assertTrue(unsupported.content().contains("404"));
        } finally {
            SessionManager.invalidateSession(session);
        }
    }

    @Test
    void profileHandlesAuthorizationGetInvalidAndValidUpdates() throws Exception {
        ProfileHandler handler = new ProfileHandler();
        UserRepository repo = mock(UserRepository.class);
        ProfileService service = mock(ProfileService.class);
        TestSupport.inject(handler, "userRepository", repo);
        TestSupport.inject(handler, "profileService", service);

        var anonymous = TestSupport.request("GET", "/profile", null, "");
        handler.handle(anonymous.exchange());
        assertEquals("/login", anonymous.headers().getFirst("Location"));

        String session = SessionManager.createSession(7L);
        try {
            var missing = TestSupport.request("GET", "/profile", "SESSION_ID=" + session, "");
            when(repo.findById(7L)).thenReturn(Optional.empty());
            handler.handle(missing.exchange());
            assertEquals("/login", missing.headers().getFirst("Location"));

            when(repo.findById(7L))
                    .thenReturn(Optional.of(new User(7L, "T", "T", "test@t.com", "secret")));
            var get = TestSupport.request("GET", "/profile", "SESSION_ID=" + session, "");
            handler.handle(get.exchange());
            assertTrue(get.content().contains("value=\"T\""));

            when(service.updateProfile(eq(7L), any(ProfileUpdateRequest.class)))
                    .thenReturn(new ProfileUpdateResult(List.of("Invalid name."), false))
                    .thenReturn(new ProfileUpdateResult(List.of(), true));
            String body = "firstName=T&lastName=T&currentPassword=&newPassword=";
            var invalid = TestSupport.request("POST", "/profile", "SESSION_ID=" + session, body);
            handler.handle(invalid.exchange());
            assertTrue(invalid.content().contains("Invalid name."));
            assertTrue(invalid.content().contains("value=\"T\""));

            var valid = TestSupport.request("POST", "/profile", "SESSION_ID=" + session, body);
            handler.handle(valid.exchange());
            assertTrue(valid.content().contains("Profile updated successfully!"));
            assertTrue(valid.content().contains("value=\"T\""));

            var other = TestSupport.request("DELETE", "/profile", "SESSION_ID=" + session, "");
            handler.handle(other.exchange());
            assertTrue(other.content().contains("404"));
        } finally {
            SessionManager.invalidateSession(session);
        }
    }

    @Test
    void logoutClearsSessionAndRedirects() throws Exception {
        String session = SessionManager.createSession(8L);
        var request = TestSupport.request("GET", "/logout", "SESSION_ID=" + session, "");
        new LogoutHandler().handle(request.exchange());
        assertFalse(SessionManager.isLogged(session));
        assertEquals("/", request.headers().getFirst("Location"));
        verify(request.exchange()).sendResponseHeaders(302, -1);
    }

    @Test
    void rendererCoversMissingFilePlaceholdersAndContentTypes() throws Exception {
        var missing = TestSupport.request("GET", "/no-such-file", null, "");
        ViewRenderer.renderFile(missing.exchange(), "missing-file.txt");
        assertEquals("File Not Found", missing.content());
        verify(missing.exchange()).sendResponseHeaders(eq(404), anyLong());
        assertTrue(missing.headers().getFirst("Content-Type").startsWith("text/plain"));

        var content = TestSupport.request("GET", "/profile", null, "");
        ViewRenderer.renderTemplate(content.exchange(), "/profile.html", Map.of(
                "ERROR_MESSAGE", "Bad input",
                "SUCCESS_MESSAGE", "Saved",
                "FIRST_NAME", "T",
                "LAST_NAME", "T"));
        assertTrue(content.content().contains("alert-danger"));
        assertTrue(content.content().contains("alert-success"));
        assertTrue(content.content().contains("Bad input"));
        assertTrue(content.content().contains("Saved"));
        assertFalse(content.content().contains("{{FIRST_NAME}}"));

        var cleared = TestSupport.request("GET", "/profile", null, "");
        ViewRenderer.renderTemplate(cleared.exchange(), "profile.html", Map.of(
                "ERROR_MESSAGE", "", "SUCCESS_MESSAGE", "",
                "FIRST_NAME", "", "LAST_NAME", ""));
        assertFalse(cleared.content().contains("{{SUCCESS_MESSAGE}}"));
        assertFalse(cleared.content().contains("alert-success"));
    }
}
