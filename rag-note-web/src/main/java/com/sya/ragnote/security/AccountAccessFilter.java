package com.sya.ragnote.security;

import com.sya.ragnote.enums.result.AuthResultEnum;
import com.sya.ragnote.exception.ServiceSystemException;
import com.sya.ragnote.service.auth.AuthService;
import com.sya.ragnote.web.Responses;

import jakarta.servlet.*;
import jakarta.servlet.http.*;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class AccountAccessFilter extends OncePerRequestFilter {
    private final AuthService authService;
    private final Responses responses;

    public AccountAccessFilter(AuthService authService, Responses responses) {
        this.authService = authService;
        this.responses = responses;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String path = request.getServletPath();
        if (path == null || path.isEmpty()) {
            String uri = request.getRequestURI();
            String context = request.getContextPath();
            path = context.isEmpty() ? uri : uri.substring(context.length());
        }
        boolean anonymousAllowed =
                path.equals("/api/auth/csrf")
                        || path.equals("/api/auth/login")
                        || path.equals("/api/auth/register")
                        || path.equals("/actuator/health");
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null
                && auth.isAuthenticated()
                && auth.getPrincipal() instanceof LoginUser user) {
            try {
                authService.me(user.id());
            } catch (ServiceSystemException e) {
                SecurityContextHolder.clearContext();
                var session = request.getSession(false);
                if (session != null) {
                    session.invalidate();
                }
                responses.error(response, e.getResultCode());
                return;
            }
        } else if (!anonymousAllowed) {
            responses.error(response, AuthResultEnum.NOT_AUTHENTICATED);
            return;
        }
        chain.doFilter(request, response);
    }
}
