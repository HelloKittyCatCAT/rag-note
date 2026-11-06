package com.sya.ragnote.security;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

import com.sya.ragnote.config.SecurityConfig;
import com.sya.ragnote.controller.auth.AuthController;
import com.sya.ragnote.service.auth.AuthService;
import com.sya.ragnote.web.Responses;

import org.junit.jupiter.api.*;
import org.springframework.context.annotation.*;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

class SecurityHttpTest {
    AnnotationConfigWebApplicationContext context;
    MockMvc mvc;

    @Configuration
    @EnableWebSecurity
    @EnableWebMvc
    @Import({SecurityConfig.class, AuthController.class})
    static class Config {
        @Bean
        AuthService authService() {
            return mock(AuthService.class);
        }

        @Bean
        Responses responses() {
            return new Responses();
        }
    }

    @BeforeEach
    void setup() {
        context = new AnnotationConfigWebApplicationContext();
        context.setServletContext(new MockServletContext());
        context.register(Config.class);
        context.refresh();
        mvc = webAppContextSetup(context).apply(springSecurity()).build();
    }

    @AfterEach
    void close() {
        context.close();
    }

    @Test
    void anonymousProtectedWriteAndLogoutReturn401BeforeCsrf() throws Exception {
        mvc.perform(post("/api/notes/delete"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(10001));
        mvc.perform(post("/api/auth/logout"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(10001));
    }

    @Test
    void publicLoginAndRegisterStillRequireCsrf() throws Exception {
        mvc.perform(post("/api/auth/login").contentType("application/json").content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(90003));
        mvc.perform(post("/api/auth/register").contentType("application/json").content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(90003));
        verifyNoInteractions(context.getBean(AuthService.class));
    }

    @Test
    void anonymousCsrfEndpointIssuesExpectedHeaderToken() throws Exception {
        mvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.headerName").value("X-XSRF-TOKEN"))
                .andExpect(jsonPath("$.data.token").isNotEmpty());
    }
}
