package com.sya.ragnote.security;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.sya.ragnote.enums.result.AuthResultEnum;
import com.sya.ragnote.enums.user.UserRole;
import com.sya.ragnote.exception.ServiceSystemException;
import com.sya.ragnote.service.auth.AuthService;
import com.sya.ragnote.web.Responses;

import jakarta.servlet.FilterChain;

import org.junit.jupiter.api.*;
import org.springframework.mock.web.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

class AccountAccessFilterTest {
    final AuthService auth = mock(AuthService.class);
    final Responses responses = mock(Responses.class);

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void anonymousLogoutFailsAuthenticationBeforeCsrf() throws Exception {
        var request = new MockHttpServletRequest("POST", "/api/auth/logout");
        request.setServletPath("/api/auth/logout");
        var response = new MockHttpServletResponse();
        var chain = mock(FilterChain.class);
        new AccountAccessFilter(auth, responses).doFilter(request, response, chain);
        verify(responses).error(response, AuthResultEnum.NOT_AUTHENTICATED);
        verifyNoInteractions(chain);
    }

    @Test
    void emptyServletPathUsesContextRelativeUriWithoutPrefixBypass() throws Exception {
        var publicRequest = new MockHttpServletRequest("GET", "/notes/api/auth/csrf");
        publicRequest.setContextPath("/notes");
        var response = new MockHttpServletResponse();
        var chain = mock(FilterChain.class);
        new AccountAccessFilter(auth, responses).doFilter(publicRequest, response, chain);
        verify(chain).doFilter(publicRequest, response);
        var protectedRequest = new MockHttpServletRequest("GET", "/notes/api/auth/csrf-extra");
        protectedRequest.setContextPath("/notes");
        var protectedChain = mock(FilterChain.class);
        new AccountAccessFilter(auth, responses)
                .doFilter(protectedRequest, response, protectedChain);
        verifyNoInteractions(protectedChain);
        verify(responses).error(response, AuthResultEnum.NOT_AUTHENTICATED);
    }

    @Test
    void disabledSessionIsInvalidatedBeforeContinuing() throws Exception {
        SecurityContextHolder.getContext()
                .setAuthentication(
                        UsernamePasswordAuthenticationToken.authenticated(
                                new LoginUser(12, "alice", UserRole.USER), null, List.of()));
        when(auth.me(12)).thenThrow(new ServiceSystemException(AuthResultEnum.ACCOUNT_DISABLED));
        var request = new MockHttpServletRequest("GET", "/api/auth/me");
        request.setServletPath("/api/auth/me");
        var session = (MockHttpSession) request.getSession();
        var response = new MockHttpServletResponse();
        var chain = mock(FilterChain.class);
        new AccountAccessFilter(auth, responses).doFilter(request, response, chain);
        assertTrue(session.isInvalid());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(responses).error(response, AuthResultEnum.ACCOUNT_DISABLED);
        verifyNoInteractions(chain);
    }
}
