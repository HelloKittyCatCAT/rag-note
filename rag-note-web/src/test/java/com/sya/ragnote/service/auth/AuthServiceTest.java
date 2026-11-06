package com.sya.ragnote.service.auth;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.sya.ragnote.dao.user.UserDao;
import com.sya.ragnote.entity.user.User;
import com.sya.ragnote.enums.result.*;
import com.sya.ragnote.exception.ServiceSystemException;
import com.sya.ragnote.param.auth.*;
import com.sya.ragnote.service.impl.auth.AuthServiceImpl;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;

class AuthServiceTest {
    final UserDao users = mock(UserDao.class);
    final RegistrationService registration = mock(RegistrationService.class);
    final PasswordEncoder passwords = mock(PasswordEncoder.class);
    final AuthService service = new AuthServiceImpl(users, registration, passwords);

    LoginParam credentials() {
        LoginParam p = new LoginParam();
        p.setUsername("Alice");
        p.setPassword("valid-password");
        return p;
    }

    @Test
    void nonexistentUserStillChecksBcryptAndReturnsSameError() {
        var error =
                assertThrows(
                        ServiceSystemException.class, () -> service.authenticate(credentials()));
        assertEquals(AuthResultEnum.INVALID_CREDENTIALS, error.getResultCode());
        verify(users).findByUsername("alice");
        verify(passwords).matches(eq("valid-password"), startsWith("$2a$12$"));
    }

    @Test
    void wrongPasswordDoesNotRevealDisabledState() {
        User u = new User();
        u.setPasswordHash("hash");
        u.setEnabled(false);
        when(users.findByUsername("alice")).thenReturn(u);
        assertEquals(
                AuthResultEnum.INVALID_CREDENTIALS,
                assertThrows(
                                ServiceSystemException.class,
                                () -> service.authenticate(credentials()))
                        .getResultCode());
    }

    @Test
    void disabledUserWithCorrectPasswordIsRejected() {
        User u = new User();
        u.setPasswordHash("hash");
        u.setEnabled(false);
        when(users.findByUsername("alice")).thenReturn(u);
        when(passwords.matches("valid-password", "hash")).thenReturn(true);
        assertEquals(
                AuthResultEnum.ACCOUNT_DISABLED,
                assertThrows(
                                ServiceSystemException.class,
                                () -> service.authenticate(credentials()))
                        .getResultCode());
    }

    @Test
    void databaseConflictIsUniformRegistrationFailureAndKeepsCause() {
        RegisterParam p = new RegisterParam();
        p.setUsername("Alice");
        p.setPassword("valid-password");
        p.setInviteToken("a".repeat(43));
        when(passwords.encode(p.getPassword())).thenReturn("hash");
        DuplicateKeyException cause = new DuplicateKeyException("conflict");
        when(registration.createUser(eq("alice"), eq("hash"), anyString())).thenThrow(cause);
        var error = assertThrows(ServiceSystemException.class, () -> service.register(p));
        assertEquals(AuthResultEnum.REGISTRATION_FAILED, error.getResultCode());
        assertSame(cause, error.getCause());
    }
}
