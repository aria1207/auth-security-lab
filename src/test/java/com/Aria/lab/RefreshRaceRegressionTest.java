package com.Aria.lab;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest
@AutoConfigureMockMvc
class RefreshRaceRegressionTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    private String loginAndGetRefreshToken(String username, String password) throws Exception {
        var res = mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"username":"%s","password":"%s"}
                            """.formatted(username, password)))
                .andReturn()
                .getResponse();

        JsonNode json = objectMapper.readTree(res.getContentAsString());
        return json.get("refreshToken").asText();
    }

    @Test
    void refreshRace_shouldAllowOnlyOneSuccess() throws Exception {
        String r1 = loginAndGetRefreshToken("admin", "pass");

        int threads = 10;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);

        AtomicInteger success200 = new AtomicInteger();

        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                try {
                    start.await(3, TimeUnit.SECONDS);

                    int status = mockMvc.perform(post("/refresh")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content("""
                                        {"refreshToken":"%s"}
                                        """.formatted(r1)))
                            .andReturn()
                            .getResponse()
                            .getStatus();

                    if (status == 200) success200.incrementAndGet();
                } catch (Exception ignored) {
                } finally {
                    done.countDown();
                }
            });
        }

        start.countDown();
        done.await(10, TimeUnit.SECONDS);
        pool.shutdownNow();

        assertThat(success200.get()).isEqualTo(1);
    }
}