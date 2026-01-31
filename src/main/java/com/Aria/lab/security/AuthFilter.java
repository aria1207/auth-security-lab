package com.Aria.lab.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

public class AuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    // 不拦截的路径（登录、h2 控制台、hello）
    private static final Set<String> PUBLIC_PREFIX = Set.of(
            "/login",
            "/refresh",
            "/logout",
            "/hello",
            "/h2-console"
    );

    public AuthFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        // 放行 public
        for (String p : PUBLIC_PREFIX) {
            if (path.startsWith(p)) return true;
        }
        return false;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String auth = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (auth == null || !auth.startsWith("Bearer ")) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            return;
        }

        String token = auth.substring("Bearer ".length()).trim();

        // 你 JwtService 里应该有 verify/parse 方法（名字可能不一样）
        // 这里假设：jwtService.verify(token) 会抛异常 or 返回 claims
        try {
            JwtService.JwtClaims claims = jwtService.verify(token);

            // 把 user 信息放到 request 里，后面 controller 想用可以取
            request.setAttribute("userId", claims.userId());
            request.setAttribute("username", claims.username());

            filterChain.doFilter(request, response);
        } catch (Exception e) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
        }
    }
}