package com.sya.ragnote.config;

import com.sya.ragnote.enums.result.*;
import com.sya.ragnote.security.AccountAccessFilter;
import com.sya.ragnote.service.auth.AuthService;
import com.sya.ragnote.web.Responses;

import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.session.*;
import org.springframework.security.web.context.*;
import org.springframework.security.web.csrf.*;

@Configuration
public class SecurityConfig {
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    HttpSessionCsrfTokenRepository csrfTokenRepository() {
        HttpSessionCsrfTokenRepository csrf = new HttpSessionCsrfTokenRepository();
        csrf.setHeaderName("X-XSRF-TOKEN");
        return csrf;
    }

    @Bean
    SessionAuthenticationStrategy sessionAuthenticationStrategy(
            HttpSessionCsrfTokenRepository csrf) {
        return new CompositeSessionAuthenticationStrategy(
                java.util.List.of(
                        new ChangeSessionIdAuthenticationStrategy(),
                        new CsrfAuthenticationStrategy(csrf)));
    }

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            Responses responses,
            AuthService authService,
            SecurityContextRepository contexts,
            HttpSessionCsrfTokenRepository csrf)
            throws Exception {
        CsrfTokenRequestAttributeHandler handler = new CsrfTokenRequestAttributeHandler();
        http.securityContext(c -> c.securityContextRepository(contexts).requireExplicitSave(true))
                .csrf(c -> c.csrfTokenRepository(csrf).csrfTokenRequestHandler(handler))
                .authorizeHttpRequests(
                        a ->
                                a.requestMatchers(
                                                "/api/auth/csrf",
                                                "/api/auth/register",
                                                "/api/auth/login",
                                                "/actuator/health")
                                        .permitAll()
                                        .requestMatchers("/api/admin/**")
                                        .hasRole("ADMIN")
                                        .anyRequest()
                                        .authenticated())
                .exceptionHandling(
                        e ->
                                e.authenticationEntryPoint(
                                                (req, res, ex) ->
                                                        responses.error(
                                                                res,
                                                                AuthResultEnum.NOT_AUTHENTICATED))
                                        .accessDeniedHandler(
                                                (req, res, ex) ->
                                                        responses.error(
                                                                res,
                                                                ex instanceof CsrfException
                                                                        ? CommonResultEnum
                                                                                .CSRF_VALIDATION_FAILED
                                                                        : AuthResultEnum
                                                                                .PERMISSION_DENIED)))
                .logout(
                        l ->
                                l.logoutUrl("/api/auth/logout")
                                        .logoutSuccessHandler(
                                                (req, res, auth) -> {
                                                    res.setContentType("application/json");
                                                    responses.writeOk(res, null);
                                                })
                                        .invalidateHttpSession(true)
                                        .clearAuthentication(true)
                                        .deleteCookies("SESSION"))
                .requestCache(c -> c.disable())
                .addFilterBefore(new AccountAccessFilter(authService, responses), CsrfFilter.class);
        return http.build();
    }
}
