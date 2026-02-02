package com.Aria.lab;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AuthorizationRegressionTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    private String loginAndGetAccessToken(String username, String password) throws Exception {
        String body = """
            {"username":"%s","password":"%s"}
            """.formatted(username, password);

        String resp = mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode json = objectMapper.readTree(resp);
        return json.get("accessToken").asText();
    }

    @Test
    void henry_cannotReadAdminUser_shouldReturn403() throws Exception {
        String token = loginAndGetAccessToken("henry", "pass");

        mockMvc.perform(get("/users/1")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("forbidden"));
    }

    @Test
    void henry_cannotUpdateAdminUser_shouldReturn403() throws Exception {
        String token = loginAndGetAccessToken("henry", "pass");

        mockMvc.perform(put("/users/1")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"username":"admin_pwned","password":"pass"}
                            """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("forbidden"));
    }

    @Test
    void henry_canReadSelf_shouldReturn200() throws Exception {
        String token = loginAndGetAccessToken("henry", "pass");

        mockMvc.perform(get("/users/2")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.username").value("henry"));
    }

    @Test
    void henry_cannotListUsers_shouldReturn403() throws Exception {
        String token = loginAndGetAccessToken("henry", "pass");

        mockMvc.perform(get("/users")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("forbidden"));
    }
}