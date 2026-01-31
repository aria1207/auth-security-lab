package com.Aria.lab;

import com.Aria.lab.security.AuthFilter;
import com.Aria.lab.security.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootApplication
public class LabApplication {

    public static void main(String[] args) {
        SpringApplication.run(LabApplication.class, args);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // 用配置注入 secret / ttl（没有就先给默认值）
    @Bean
    public JwtService jwtService(
            @Value("${app.jwt.secret:CHANGE_ME_TO_A_LONG_RANDOM_SECRET}") String secret,
            @Value("${app.jwt.ttlSeconds:3600}") long ttlSeconds
    ) {
        return new JwtService(secret, ttlSeconds);
    }

    @Bean
    public FilterRegistrationBean<AuthFilter> authFilter(JwtService jwtService) {
        FilterRegistrationBean<AuthFilter> reg = new FilterRegistrationBean<>();
        reg.setFilter(new AuthFilter(jwtService));
        reg.addUrlPatterns("/*");
        reg.setOrder(1);
        return reg;
    }
}