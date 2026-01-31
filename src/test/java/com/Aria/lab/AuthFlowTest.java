package com.Aria.lab;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AuthFlowTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    // 和 data.sql 里的账号一致
    private static final String USERNAME = "admin";
    private static final String PASSWORD = "pass";

    /**
     * 1️⃣ 登录必须返回 accessToken + refreshToken
     */
    @Test
    void login_returns_access_and_refresh_token() throws Exception {
        String body = """
                {"username":"%s","password":"%s"}
                """.formatted(USERNAME, PASSWORD);

        mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty());
    }

    /**
     * 2️⃣ refresh token rotation：旧 refresh 立刻失效
     */
    @Test
    void refresh_rotation_old_refresh_token_should_fail() throws Exception {
        Tokens first = login();
        Tokens second = refresh(first.refreshToken);

        assertThat(second.refreshToken).isNotEqualTo(first.refreshToken);

        // 再用旧 refreshToken -> 401
        mockMvc.perform(post("/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"%s"}
                                """.formatted(first.refreshToken)))
                .andExpect(status().isUnauthorized());
    }

    /**
     * 3️⃣ logout 后 refreshToken 必须失效
     */
    @Test
    void logout_should_revoke_refresh_token() throws Exception {
        Tokens tokens = login();

        // logout
        mockMvc.perform(post("/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"%s"}
                                """.formatted(tokens.refreshToken)))
                .andExpect(status().isNoContent());

        // 再 refresh -> 401
        mockMvc.perform(post("/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"%s"}
                                """.formatted(tokens.refreshToken)))
                .andExpect(status().isUnauthorized());
    }

    /**
     * 4️⃣ refresh 缺参数 -> 400
     */
    @Test
    void refresh_missing_token_should_return_400() throws Exception {
        mockMvc.perform(post("/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    /* ================= helpers ================= */

    private Tokens login() throws Exception {
        String resp = mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"%s"}
                                """.formatted(USERNAME, PASSWORD)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode json = objectMapper.readTree(resp);
        return new Tokens(
                json.get("accessToken").asText(),
                json.get("refreshToken").asText()
        );
    }

    private Tokens refresh(String refreshToken) throws Exception {
        String resp = mockMvc.perform(post("/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"%s"}
                                """.formatted(refreshToken)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode json = objectMapper.readTree(resp);
        return new Tokens(
                json.get("accessToken").asText(),
                json.get("refreshToken").asText()
        );
    }

    private record Tokens(String accessToken, String refreshToken) {}
}