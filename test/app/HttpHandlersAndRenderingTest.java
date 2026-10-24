package app;

import app.dto.*;
import app.handler.*;
import app.model.User;
import app.repository.UserRepository;
import app.service.AuthService;
import app.service.ProfileService;
import app.service.RegistrationService;
import app.util.SessionManager;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class HttpHandlersAndRenderingTest {

    @Test
    void indexShowsHomePageAndRedirectsLoggedInUser() throws Exception {
        IndexHandler handler = new IndexHandler();
        var anonymous = TestSupport.request("GET", "/", null, "");
        handler.handle(anonymous.exchange());
        assertTrue(anonymous.content().contains("Welcome"));

        var css = TestSupport.request("GET", "/css/app.css", null, "");
        handler.handle(css.exchange());
        assertTrue(css.headers().getFirst("Content-Type").startsWith("text/css"));

        var invalid = TestSupport.request("GET", "/invalid", null, "");
        handler.handle(invalid.exchange());
        assertTrue(invalid.content().contains("404"));

        var unsupported = TestSupport.request("POST", "/", null, "");
        handler.handle(unsupported.exchange());
        assertTrue(unsupported.content().contains("404"));

        String session = SessionManager.createSession(1L);
        try {
            var loggedIn = TestSupport.request("GET", "/", "SESSION_ID=" + session, "");
            handler.handle(loggedIn.exchange());
            assertEquals("/home", loggedIn.headers().getFirst("Location"));
        } finally {
            SessionManager.invalidateSession(session);
        }
    }

    @Test
    void loginFormHandlesRejectedAndAcceptedCredentials() throws Exception {
        AuthService auth = mock(AuthService.class);
        LoginHandler handler = new LoginHandler();
        TestSupport.inject(handler, "authService", auth);

        var get = TestSupport.request("GET", "/login", null, "");
        handler.handle(get.exchange());
        assertTrue(get.content().contains("Sign In"));

        when(auth.login(any(LoginRequest.class)))
                .thenReturn(new LoginResult(List.of("Invalid email or password."), null))
                .thenReturn(new LoginResult(List.of(), "session1"));

        var rejected = TestSupport.request("POST", "/login", null,
                "email=test.com&password=wrong");
        handler.handle(rejected.exchange());
        assertTrue(rejected.content().contains("Invalid email or password."));

        var accepted = TestSupport.request("POST", "/login", null,
                "email=test%40t.com&password=secret1");
        handler.handle(accepted.exchange());
        assertEquals("/home", accepted.headers().getFirst("Location"));
        assertTrue(accepted.headers().getFirst("Set-Cookie").contains("SESSION_ID=session1"));
        verify(auth, times(2)).login(any(LoginRequest.class));

        var unsupported = TestSupport.request("DELETE", "/login", null, "");
        handler.handle(unsupported.exchange());
        assertTrue(unsupported.content().contains("404"));
    }

    @Test
    void registrationFormHandlesRejectedAndAcceptedRequests() throws Exception {
        RegistrationService registration = mock(RegistrationService.class);
        RegistrationHandler handler = new RegistrationHandler();
        TestSupport.inject(handler, "registrationService", registration);

        var get = TestSupport.request("GET", "/register", null, "");
        handler.handle(get.exchange());
        assertTrue(get.content().contains("Create Account"));
        assertFalse(get.content().contains("{{CAPTCHA_TOKEN}}"));

        when(registration.register(any(RegisterRequest.class)))
                .thenReturn(new RegistrationResult(List.of("Invalid email format."), null))
                .thenReturn(new RegistrationResult(List.of(), "session2"));

        String form = "firstName=T&lastName=T&email=test%40t.com" +
                "&password=secret1&confirmPassword=secret1&captchaAnswer=4&captchaToken=token";
        var rejected = TestSupport.request("POST", "/register", null,
                form.replace("test%40t.com", "test.com"));
        handler.handle(rejected.exchange());
        assertTrue(rejected.content().contains("Invalid email format."));

        var accepted = TestSupport.request("POST", "/register", null, form);
        handler.handle(accepted.exchange());
        assertEquals("/home", accepted.headers().getFirst("Location"));
        assertTrue(accepted.headers().getFirst("Set-Cookie").contains("SESSION_ID=session2"));

        var unsupported = TestSupport.request("PUT", "/register", null, "");
        handler.handle(unsupported.exchange());
        assertTrue(unsupported.content().contains("404"));
    }

    @Test
    void homeRedirectsGuestsAndDisplaysLoggedInUser() throws Exception {
        HomeHandler handler = new HomeHandler();
        UserRepository repo = mock(UserRepository.class);
        TestSupport.inject(handler, "userRepository", repo);

        var guest = TestSupport.request("GET", "/home", null, "");
        handler.handle(guest.exchange());
        assertEquals("/", guest.headers().getFirst("Location"));

        String session = SessionManager.createSession(7L);
        try {
            when(repo.findById(7L)).thenReturn(Optional.empty());
            var missing = TestSupport.request("GET", "/home", "SESSION_ID=" + session, "");
            handler.handle(missing.exchange());
            assertEquals("/", missing.headers().getFirst("Location"));

            when(repo.findById(7L))
                    .thenReturn(Optional.of(new User(7L, "T", "T", "test@t.com", "secret1")));
            var loggedIn = TestSupport.request("GET", "/home", "SESSION_ID=" + session, "");
            handler.handle(loggedIn.exchange());
            assertTrue(loggedIn.content().contains("Welcome, T"));

            var unsupported = TestSupport.request("PUT", "/home", "SESSION_ID=" + session, "");
            handler.handle(unsupported.exchange());
            assertTrue(unsupported.content().contains("404"));
        } finally {
            SessionManager.invalidateSession(session);
        }
    }

    @Test
    void profileRequiresLoginAndHandlesUpdates() throws Exception {
        ProfileHandler handler = new ProfileHandler();
        UserRepository repo = mock(UserRepository.class);
        ProfileService profileService = mock(ProfileService.class);
        TestSupport.inject(handler, "userRepository", repo);
        TestSupport.inject(handler, "profileService", profileService);

        var guest = TestSupport.request("GET", "/profile", null, "");
        handler.handle(guest.exchange());
        assertEquals("/login", guest.headers().getFirst("Location"));

        String session = SessionManager.createSession(7L);
        try {
            when(repo.findById(7L)).thenReturn(Optional.empty());
            var missing = TestSupport.request("GET", "/profile", "SESSION_ID=" + session, "");
            handler.handle(missing.exchange());
            assertEquals("/login", missing.headers().getFirst("Location"));

            when(repo.findById(7L))
                    .thenReturn(Optional.of(new User(7L, "T", "T", "test@t.com", "secret1")));
            var get = TestSupport.request("GET", "/profile", "SESSION_ID=" + session, "");
            handler.handle(get.exchange());
            assertTrue(get.content().contains("value=\"T\""));

            when(profileService.updateProfile(eq(7L), any(ProfileUpdateRequest.class)))
                    .thenReturn(new ProfileUpdateResult(List.of("Invalid name."), false))
                    .thenReturn(new ProfileUpdateResult(List.of(), true));
            String form = "firstName=T&lastName=T&currentPassword=&newPassword=";

            var rejected = TestSupport.request("POST", "/profile", "SESSION_ID=" + session, form);
            handler.handle(rejected.exchange());
            assertTrue(rejected.content().contains("Invalid name."));

            var accepted = TestSupport.request("POST", "/profile", "SESSION_ID=" + session, form);
            handler.handle(accepted.exchange());
            assertTrue(accepted.content().contains("Profile updated successfully!"));

            var unsupported = TestSupport.request("DELETE", "/profile", "SESSION_ID=" + session, "");
            handler.handle(unsupported.exchange());
            assertTrue(unsupported.content().contains("404"));
        } finally {
            SessionManager.invalidateSession(session);
        }
    }

    @Test
    void logoutRemovesSession() throws Exception {
        String session = SessionManager.createSession(8L);
        var request = TestSupport.request("GET", "/logout", "SESSION_ID=" + session, "");
        new LogoutHandler().handle(request.exchange());
        assertFalse(SessionManager.isLogged(session));
        assertEquals("/", request.headers().getFirst("Location"));
        verify(request.exchange()).sendResponseHeaders(302, -1);
    }
}
