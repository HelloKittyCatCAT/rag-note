package com.sya.ragnote.integration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.sya.ragnote.config.BootstrapAdminInitializer;
import com.sya.ragnote.config.DatabaseSchemaInitializer;
import com.sya.ragnote.dao.user.UserDao;
import com.sya.ragnote.entity.user.User;
import com.sya.ragnote.enums.user.UserRole;

import jakarta.servlet.http.Cookie;

import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import tools.jackson.databind.json.JsonMapper;

/** H2 仅验证应用装配和 JDBC Session，不代替 PostgreSQL/pgvector 集成验证。 */
@SpringBootTest(
        properties = {
            "spring.datasource.url=jdbc:h2:mem:wiring;DB_CLOSE_DELAY=-1",
            "spring.datasource.driver-class-name=org.h2.Driver",
            "spring.datasource.username=sa",
            "spring.datasource.password=",
            "spring.flyway.enabled=false",
            "spring.session.jdbc.initialize-schema=always",
            "rag.models.chat.api-key=test-only",
            "rag.models.chat.model=test-model",
            "rag.models.embedding.api-key=test-only",
            "rag.models.embedding.model=test-embedding"
        })
@AutoConfigureMockMvc
class ApplicationWiringTest {
    @Autowired MockMvc mvc;
    @Autowired PasswordEncoder passwords;
    @Autowired SqlSessionFactory sql;
    @MockitoBean UserDao users;
    @MockitoBean BootstrapAdminInitializer bootstrap;
    @MockitoBean DatabaseSchemaInitializer schemaInitializer;
    private final JsonMapper json = JsonMapper.builder().build();

    @BeforeEach
    void user() {
        User user = new User();
        user.setId(7L);
        user.setUsername("note_user");
        user.setRole(UserRole.USER);
        user.setEnabled(true);
        user.setPasswordHash(passwords.encode("test-password-123"));
        when(users.findByUsername("note_user")).thenReturn(user);
        when(users.findById(7)).thenReturn(user);
    }

    @Test
    void fullApplicationLoadsAllMapperXmlAndUsesRealJdbcSessionAcrossRequests() throws Exception {
        assertTrue(
                sql.getConfiguration()
                        .hasStatement("com.sya.ragnote.mapper.note.NoteChunkMapper.search"));
        var csrf = mvc.perform(get("/api/auth/csrf")).andExpect(status().isOk()).andReturn();
        Cookie anonymous = csrf.getResponse().getCookie("SESSION");
        assertNotNull(anonymous);
        String token =
                json.readTree(csrf.getResponse().getContentAsString()).at("/data/token").asString();
        var login =
                mvc.perform(
                                post("/api/auth/login")
                                        .cookie(anonymous)
                                        .header("X-XSRF-TOKEN", token)
                                        .contentType("application/json")
                                        .content(
                                                "{\"username\":\"note_user\",\"password\":\"test-password-123\"}"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.id").value("7"))
                        .andReturn();
        Cookie authenticated = login.getResponse().getCookie("SESSION");
        assertNotNull(authenticated);
        assertNotEquals(anonymous.getValue(), authenticated.getValue());
        var me =
                mvc.perform(get("/api/auth/me").cookie(authenticated))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.username").value("note_user"))
                        .andReturn();
        assertEquals(
                me.getResponse().getHeader("X-Request-Id"),
                json.readTree(me.getResponse().getContentAsString()).path("requestId").asString());
        mvc.perform(post("/api/admin/invitations").cookie(authenticated))
                .andExpect(status().isForbidden());
        var after =
                mvc.perform(get("/api/auth/csrf").cookie(authenticated))
                        .andExpect(status().isOk())
                        .andReturn();
        String refreshed =
                json.readTree(after.getResponse().getContentAsString())
                        .at("/data/token")
                        .asString();
        mvc.perform(
                        post("/api/auth/logout")
                                .cookie(authenticated)
                                .header("X-XSRF-TOKEN", refreshed))
                .andExpect(status().isOk());
        mvc.perform(get("/api/auth/me").cookie(authenticated)).andExpect(status().isUnauthorized());
    }

    @Test
    void unknownJsonFieldsAreRejectedByRuntimeConfiguration() throws Exception {
        var csrf = mvc.perform(get("/api/auth/csrf")).andReturn();
        String token =
                json.readTree(csrf.getResponse().getContentAsString()).at("/data/token").asString();
        mvc.perform(
                        post("/api/auth/login")
                                .cookie(csrf.getResponse().getCookie("SESSION"))
                                .header("X-XSRF-TOKEN", token)
                                .contentType("application/json")
                                .content(
                                        "{\"username\":\"note_user\",\"password\":\"test-password-123\",\"role\":\"ADMIN\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(90001));
    }
}
