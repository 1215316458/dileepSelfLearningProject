package com.ecommerce.user_service.controller;

import com.ecommerce.user_service.dto.AuthRequest;
import com.ecommerce.user_service.dto.AuthResponse;
import com.ecommerce.user_service.dto.RegisterRequest;
import com.ecommerce.user_service.security.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// WebEnvironment.MOCK — loads full Spring MVC context without starting a real server
// MockMvc built manually with springSecurity() to include the security filter chain
@SpringBootTest(webEnvironment = WebEnvironment.MOCK)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
class AuthControllerTest {

    @Autowired WebApplicationContext context;
    @Autowired JwtService            jwtService;

    private final ObjectMapper mapper = new ObjectMapper();

    // build MockMvc with the full security filter chain applied
    private MockMvc mockMvc() {
        return MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    void register_returnsTokens() throws Exception {
        RegisterRequest req = new RegisterRequest("testuser", "test@example.com", "password123");

        MvcResult result = mockMvc().perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(req)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.accessToken").isNotEmpty())
            .andExpect(jsonPath("$.refreshToken").isNotEmpty())
            .andExpect(jsonPath("$.tokenType").value("Bearer"))
            .andReturn();

        AuthResponse response = mapper.readValue(
            result.getResponse().getContentAsString(), AuthResponse.class);
        assertThat(jwtService.extractUsername(response.accessToken())).isEqualTo("testuser");
    }

    @Test
    void register_duplicateEmail_returns400() throws Exception {
        RegisterRequest req = new RegisterRequest("user2", "dup@example.com", "password123");
        mockMvc().perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(req)))
            .andExpect(status().isCreated());

        RegisterRequest dup = new RegisterRequest("user3", "dup@example.com", "password123");
        mockMvc().perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(dup)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void login_validCredentials_returnsTokens() throws Exception {
        RegisterRequest reg = new RegisterRequest("loginuser", "login@example.com", "password123");
        mockMvc().perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(reg)))
            .andExpect(status().isCreated());

        AuthRequest login = new AuthRequest("loginuser", "password123");
        mockMvc().perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(login)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").isNotEmpty());
    }

    @Test
    void login_wrongPassword_returns401() throws Exception {
        RegisterRequest reg = new RegisterRequest("badpassuser", "badpass@example.com", "password123");
        mockMvc().perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(reg)))
            .andExpect(status().isCreated());

        AuthRequest bad = new AuthRequest("badpassuser", "wrongpassword");
        mockMvc().perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(bad)))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void profile_withValidToken_returnsUser() throws Exception {
        RegisterRequest reg = new RegisterRequest("profileuser", "profile@example.com", "password123");
        MvcResult regResult = mockMvc().perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(reg)))
            .andExpect(status().isCreated())
            .andReturn();

        AuthResponse tokens = mapper.readValue(
            regResult.getResponse().getContentAsString(), AuthResponse.class);

        mockMvc().perform(get("/api/users/profile")
                .header("Authorization", "Bearer " + tokens.accessToken()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.username").value("profileuser"))
            .andExpect(jsonPath("$.email").value("profile@example.com"));
    }

    @Test
    void profile_withoutToken_returns401() throws Exception {
        mockMvc().perform(get("/api/users/profile"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void refresh_withValidRefreshToken_returnsNewTokens() throws Exception {
        RegisterRequest reg = new RegisterRequest("refreshuser", "refresh@example.com", "password123");
        MvcResult regResult = mockMvc().perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(reg)))
            .andExpect(status().isCreated())
            .andReturn();

        AuthResponse tokens = mapper.readValue(
            regResult.getResponse().getContentAsString(), AuthResponse.class);

        mockMvc().perform(post("/api/auth/refresh")
                .header("Refresh-Token", tokens.refreshToken()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").isNotEmpty());
    }
}
