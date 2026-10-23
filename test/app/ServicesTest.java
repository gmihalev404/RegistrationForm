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

    private static RegisterRequest registerRequest() {
        CaptchaService.CaptchaChallenge captcha = CaptchaService.generateChallenge();
        return new RegisterRequest("T", "T", "test@t.com", "secret1",
                "secret1", captcha.question(), TestSupport.captchaAnswer(captcha), captcha.token());
    }

    @Test
    void authenticationRejectsBadInputAndCredentialsAndCreatesAndRemovesSession() throws Exception {
        UserRepository repo = mock(UserRepository.class);
        AuthService service = new AuthService();
        TestSupport.inject(service, "userRepository", repo);

        LoginResult invalid = service.login(new LoginRequest("", ""));
        assertFalse(invalid.isSuccessful());
        verifyNoInteractions(repo);

        when(repo.findByEmailAndPassword("test@t.com", "wrong"))
                .thenReturn(Optional.empty());
        assertFalse(service.login(new LoginRequest("test@t.com", "wrong")).isSuccessful());

        when(repo.findByEmailAndPassword("test@t.com", "secret1"))
                .thenReturn(Optional.of(new User(7L, "T", "T", "test@t.com", "secret1")));
        LoginResult success = service.login(new LoginRequest("test@t.com", "secret1"));
        assertTrue(success.isSuccessful());
        assertEquals(7L, SessionManager.getUserId(success.sessionId()).orElseThrow());

        service.logout(null);
        service.logout("  ");
        assertTrue(SessionManager.isLogged(success.sessionId()));
        service.logout(success.sessionId());
        assertFalse(SessionManager.isLogged(success.sessionId()));
    }

    @Test
    void registrationCoversValidationDuplicateEmailAndSuccess() throws Exception {
        UserRepository repo = mock(UserRepository.class);
        RegistrationService service = new RegistrationService();
        TestSupport.inject(service, "userRepository", repo);

        RegistrationResult invalid = service.register(
                new RegisterRequest(null, null, null, null, null, null, null, null));
        assertFalse(invalid.isSuccessful());
        verifyNoInteractions(repo);

        RegisterRequest duplicate = registerRequest();
        when(repo.findByEmail("test@t.com"))
                .thenReturn(Optional.of(new User(9L, "T", "T", "test@t.com", "secret1")));
        RegistrationResult duplicateResult = service.register(duplicate);
        assertFalse(duplicateResult.isSuccessful());
        assertTrue(duplicateResult.errors().getFirst().contains("already exists"));
        verify(repo, never()).save(any());

        clearInvocations(repo);
        when(repo.findByEmail("test@t.com")).thenReturn(Optional.empty());
        when(repo.findByEmailAndPassword("test@t.com", "secret1"))
                .thenReturn(Optional.of(new User(12L, "T", "T", "test@t.com", "secret1")));
        RegistrationResult success = service.register(registerRequest());
        assertTrue(success.isSuccessful());
        verify(repo).save(argThat(user ->
                user.getId() == null && user.getFirstName().equals("T") &&
                        user.getEmail().equals("test@t.com")));
        assertEquals(12L, SessionManager.getUserId(success.sessionId()).orElseThrow());
        SessionManager.invalidateSession(success.sessionId());
    }

    @Test
    void profileUpdateCoversMissingUserValidationAndNameOnlyChange() throws Exception {
        UserRepository repo = mock(UserRepository.class);
        ProfileService service = new ProfileService();
        TestSupport.inject(service, "userRepository", repo);

        when(repo.findById(99L)).thenReturn(Optional.empty());
        assertFalse(service.updateProfile(99L,
                new ProfileUpdateRequest("T", "T", null, null)).isSuccessful());

        User existing = new User(3L, null, null, "test@t.com", "oldpass");
        when(repo.findById(3L)).thenReturn(Optional.of(existing));
        assertFalse(service.updateProfile(3L,
                new ProfileUpdateRequest("", "", null, null)).isSuccessful());
        verify(repo, never()).save(any());

        ProfileUpdateResult result = service.updateProfile(3L,
                new ProfileUpdateRequest("T", "T", null, null));
        assertTrue(result.isSuccessful());
        assertEquals("T", existing.getFirstName());
        assertEquals("T", existing.getLastName());
        assertEquals("oldpass", existing.getPassword());
        verify(repo).save(existing);
    }

    @Test
    void profileUpdateCanChangePasswordWithCorrectCurrentPassword() throws Exception {
        UserRepository repo = mock(UserRepository.class);
        ProfileService service = new ProfileService();
        TestSupport.inject(service, "userRepository", repo);
        User existing = new User(3L, null, null, "test@t.com", "oldpass");
        when(repo.findById(3L)).thenReturn(Optional.of(existing));

        assertTrue(service.updateProfile(3L,
                new ProfileUpdateRequest("T", "T", "oldpass", "newpass")).isSuccessful());
        assertEquals("newpass", existing.getPassword());
        verify(repo).save(existing);
    }
}
