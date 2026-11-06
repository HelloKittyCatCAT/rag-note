package com.sya.ragnote.controller.auth;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.sya.ragnote.enums.user.UserRole;
import com.sya.ragnote.param.auth.LoginParam;
import com.sya.ragnote.security.*;
import com.sya.ragnote.service.auth.AuthService;
import com.sya.ragnote.web.Responses;

import org.junit.jupiter.api.*;
import org.springframework.mock.web.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

class AuthControllerTest {
    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void loginRotatesSessionAndPersistsAuthentication() {
        AuthService auth = mock(AuthService.class);
        Responses responses = mock(Responses.class);
        var repository = new HttpSessionSecurityContextRepository();
        var controller =
                new AuthController(
                        auth, responses, new ChangeSessionIdAuthenticationStrategy(), repository);
        LoginParam p = new LoginParam();
        p.setUsername("Alice");
        p.setPassword("valid-password");
        when(auth.authenticate(p)).thenReturn(new LoginUser(9, "alice", UserRole.USER));
        var request = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();
        String oldId = request.getSession().getId();
        request.setRequestedSessionId(oldId);
        request.setRequestedSessionIdValid(true);
        request.setRequestedSessionIdFromCookie(true);
        controller.login(p, request, response);
        assertNotEquals(oldId, request.getSession().getId());
        assertEquals(1800, request.getSession().getMaxInactiveInterval());
        var saved =
                (org.springframework.security.core.context.SecurityContext)
                        request.getSession()
                                .getAttribute(
                                        HttpSessionSecurityContextRepository
                                                .SPRING_SECURITY_CONTEXT_KEY);
        assertNotNull(saved);
        assertEquals(9, ((LoginUser) saved.getAuthentication().getPrincipal()).id());
    }
}
