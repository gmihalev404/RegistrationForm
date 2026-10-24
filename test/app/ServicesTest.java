package app;

import app.dto.*;
import app.model.User;
import app.repository.UserRepository;
import app.service.AuthService;
import app.service.CaptchaService;
import app.service.ProfileService;
import app.service.RegistrationService;
import app.util.SessionManager;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ServicesTest {

    private static RegisterRequest registrationRequest() {
        var captcha = CaptchaService.generateChallenge();
        return new RegisterRequest("T", "T", "test@t.com", "secret1", "secret1",
                captcha.question(), TestSupport.captchaAnswer(captcha), captcha.token());
    }

    @Test
    void loginWithInvalidAndValidCredentialsAndLogout() throws Exception {
        UserRepository repo = mock(UserRepository.class);
        AuthService service = new AuthService();
        TestSupport.inject(service, "userRepository", repo);

        assertFalse(service.login(new LoginRequest("", "")).isSuccessful());
        verifyNoInteractions(repo);

        when(repo.findByEmailAndPassword("test@t.com", "wrong"))
                .thenReturn(Optional.empty());
        assertFalse(service.login(new LoginRequest("test@t.com", "wrong")).isSuccessful());

        when(repo.findByEmailAndPassword("test@t.com", "secret1"))
                .thenReturn(Optional.of(new User(7L, "T", "T", "test@t.com", "secret1")));
        LoginResult success = service.login(new LoginRequest("test@t.com", "secret1"));
        assertTrue(success.isSuccessful());
        assertEquals(7L, SessionManager.getUserId(success.sessionId()).orElseThrow());

        service.logout(success.sessionId());
        assertFalse(SessionManager.isLogged(success.sessionId()));
    }

    @Test
    void registrationRejectsInvalidAndDuplicateAndSavesValidUser() throws Exception {
        UserRepository repo = mock(UserRepository.class);
        RegistrationService service = new RegistrationService();
        TestSupport.inject(service, "userRepository", repo);

        assertFalse(service.register(
                new RegisterRequest(null, null, null, null, null, null, null, null)).isSuccessful());
        verifyNoInteractions(repo);

        when(repo.findByEmail("test@t.com"))
                .thenReturn(Optional.of(new User(9L, "T", "T", "test@t.com", "secret1")));
        assertFalse(service.register(registrationRequest()).isSuccessful());
        verify(repo, never()).save(any());

        when(repo.findByEmail("test@t.com")).thenReturn(Optional.empty());
        when(repo.findByEmailAndPassword("test@t.com", "secret1"))
                .thenReturn(Optional.of(new User(12L, "T", "T", "test@t.com", "secret1")));
        RegistrationResult registered = service.register(registrationRequest());
        assertTrue(registered.isSuccessful());
        verify(repo).save(argThat(user -> user.getId() == null &&
                "T".equals(user.getFirstName()) && "test@t.com".equals(user.getEmail())));
        assertEquals(12L, SessionManager.getUserId(registered.sessionId()).orElseThrow());
        SessionManager.invalidateSession(registered.sessionId());
    }

    @Test
    void profileUpdateChecksUserAndChangesNameAndPassword() throws Exception {
        UserRepository repo = mock(UserRepository.class);
        ProfileService service = new ProfileService();
        TestSupport.inject(service, "userRepository", repo);

        when(repo.findById(99L)).thenReturn(Optional.empty());
        assertFalse(service.updateProfile(99L,
                new ProfileUpdateRequest("T", "T", null, null)).isSuccessful());

        User user = new User(3L, null, null, "test@t.com", "oldpass");
        when(repo.findById(3L)).thenReturn(Optional.of(user));
        assertFalse(service.updateProfile(3L,
                new ProfileUpdateRequest("", "", null, null)).isSuccessful());
        verify(repo, never()).save(any());

        ProfileUpdateResult nameResult = service.updateProfile(3L,
                new ProfileUpdateRequest("T", "T", null, null));
        assertTrue(nameResult.isSuccessful());
        assertEquals("T", user.getFirstName());
        assertEquals("T", user.getLastName());
        assertEquals("oldpass", user.getPassword());

        ProfileUpdateResult passwordResult = service.updateProfile(3L,
                new ProfileUpdateRequest("T", "T", "oldpass", "newpass"));
        assertTrue(passwordResult.isSuccessful());
        assertEquals("newpass", user.getPassword());
        verify(repo, times(2)).save(user);
    }
}
