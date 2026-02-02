package com.Aria.lab.controller;

import com.Aria.lab.dao.UserDao;
import com.Aria.lab.dao.RefreshTokenDao;
import com.Aria.lab.dto.UserCreateRequest;
import com.Aria.lab.dto.UserDTO;
import com.Aria.lab.model.User;
import com.Aria.lab.dto.LoginRequest;
import com.Aria.lab.dto.LoginResponse;
import com.Aria.lab.dto.RefreshRequest;
import com.Aria.lab.dto.RefreshResponse;
import com.Aria.lab.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.dao.DataIntegrityViolationException;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;
import java.util.UUID;
import java.time.temporal.ChronoUnit;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.http.converter.HttpMessageNotReadableException;
import java.time.Instant;

@RestController
public class UserController {

    private final UserDao userDao;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenDao refreshTokenDao;

    public UserController(UserDao userDao, PasswordEncoder passwordEncoder, JwtService jwtService, RefreshTokenDao refreshTokenDao) {
        this.userDao = userDao;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenDao = refreshTokenDao;
    }

    private int requireAuthUserId(HttpServletRequest req) {
        Object uid = req.getAttribute("userId");
        if (uid == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "missing auth context");
        }
        if (uid instanceof Integer i) {
            return i;
        }
        if (uid instanceof Number n) {
            return n.intValue();
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid auth context");
    }

    private String requireAuthUsername(HttpServletRequest req) {
        Object uname = req.getAttribute("username");
        if (uname == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "missing auth context");
        }
        return uname.toString();
    }

    private void requireAdmin(HttpServletRequest req) {
        String username = requireAuthUsername(req);
        if (!"admin".equals(username)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "forbidden");
        }
    }

    private void requireSelfOrAdmin(HttpServletRequest req, int targetUserId) {
        String username = requireAuthUsername(req);
        int currentUserId = requireAuthUserId(req);
        if ("admin".equals(username)) {
            return;
        }
        if (currentUserId != targetUserId) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "forbidden");
        }
    }

    @GetMapping("/hello")
    public String hello() {
        return "hello";
    }
    @GetMapping("/me")
    public UserDTO me(HttpServletRequest req) {
        Object uid = req.getAttribute("userId");
        Object uname = req.getAttribute("username");

        if (uid == null || uname == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "missing auth context");
        }

        int userId;
        if (uid instanceof Integer i) {
            userId = i;
        } else if (uid instanceof Number n) {
            userId = n.intValue();
        } else {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid auth context");
        }

        return new UserDTO(userId, uname.toString());
    }

    @GetMapping("/users")
    public List<UserDTO> users(HttpServletRequest req) {
        requireAdmin(req);
        return userDao.findAll()
                .stream()
                .map(u -> new UserDTO(u.getId(), u.getUsername()))
                .toList();
    }
    @GetMapping("/users/{id}")
    public UserDTO userById(@PathVariable int id, HttpServletRequest req) {
        requireSelfOrAdmin(req, id);
        return userDao.findById(id)
                .map(u -> new UserDTO(u.getId(), u.getUsername()))
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "User not found: id=" + id
                        )
                );
    }
    @DeleteMapping("/users/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(@PathVariable int id, HttpServletRequest req) {
        requireSelfOrAdmin(req, id);
        boolean deleted = userDao.deleteById(id);

        if (!deleted) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "User not found: id=" + id
            );
        }
    }
    @PutMapping("/users/{id}")
    public UserDTO updateUser(@PathVariable int id, @RequestBody UserCreateRequest req, HttpServletRequest httpReq) {

        requireSelfOrAdmin(httpReq, id);

        // 1) 简单校验（先不用 Validation 也可以）
        if (req.getUsername() == null || req.getUsername().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "username is required");
        }
        if (req.getPassword() == null || req.getPassword().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "password is required");
        }

        // 2) 更新（404 由这里处理；409 交给全局异常处理）
        String hashed = passwordEncoder.encode(req.getPassword());
        boolean updated = userDao.updateById(id, req.getUsername(), hashed);
        if (!updated) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found: id=" + id);
        }

        // 3) 返回 DTO（只返回 id + username）
        return new UserDTO(id, req.getUsername());
    }
    @PostMapping("/users")
    @ResponseStatus(HttpStatus.CREATED)
    public UserDTO createUser(@RequestBody UserCreateRequest req, HttpServletRequest httpReq) {

        requireAdmin(httpReq);

        // 1) 参数校验
        if (req.getUsername() == null || req.getUsername().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "username is required");
        }
        if (req.getPassword() == null || req.getPassword().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "password is required");
        }

        // 2) 调用 DAO（409 交给全局异常处理）
        String hashed = passwordEncoder.encode(req.getPassword());
        User created = userDao.create(req.getUsername(), hashed);

        // 3) 返回 DTO（只回 id + username）
        return new UserDTO(created.getId(), created.getUsername());
    }
    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest req) {
        if (req.getUsername() == null || req.getUsername().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "username is required");
        }
        if (req.getPassword() == null || req.getPassword().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "password is required");
        }

        User u = userDao.findByUsername(req.getUsername())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid credentials"));

        if (!passwordEncoder.matches(req.getPassword(), u.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid credentials");
        }

        // 1) access token（短期）
        String accessToken = jwtService.issueToken(u.getId(), u.getUsername());

        // 2) refresh token（随机字符串）
        String refreshToken = UUID.randomUUID().toString();

        // 3) refresh token 过期时间（例如 7 天）
        Instant refreshExpiresAt = Instant.now().plus(7, ChronoUnit.DAYS);

        // 4) 存数据库
        refreshTokenDao.insert(u.getId(), refreshToken, refreshExpiresAt);

        // 5) 返回 4 个参数（id, username, accessToken, refreshToken）
        return new LoginResponse(u.getId(), u.getUsername(), accessToken, refreshToken);
    }

    @PostMapping("/refresh")
    public RefreshResponse refresh(@RequestBody RefreshRequest req) {
        if (req == null || req.refreshToken() == null || req.refreshToken().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "refreshToken is required");
        }

        // 1) 原子消费旧 refresh token（未 revoked + 未过期），成功才继续
        Instant now = Instant.now();

        int userId = refreshTokenDao.consumeValidToken(req.refreshToken(), now)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid refresh token"));

        // 2) 找到用户（用于签新的 access token）
        User u = userDao.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "user not found"));

        // 3) 生成并保存新的 refresh token（例如 7 天）
        String newRefreshToken = UUID.randomUUID().toString();
        Instant newRefreshExpiresAt = now.plus(7, ChronoUnit.DAYS);
        refreshTokenDao.insert(u.getId(), newRefreshToken, newRefreshExpiresAt);

        // 4) 生成新的 access token
        String newAccessToken = jwtService.issueToken(u.getId(), u.getUsername());

        return new RefreshResponse(newAccessToken, newRefreshToken);
    }
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@RequestBody RefreshRequest req) {
        if (req == null || req.refreshToken() == null || req.refreshToken().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "refreshToken is required");
        }
        refreshTokenDao.revokeByToken(req.refreshToken());
    }
}

@RestControllerAdvice
class ApiExceptionHandler {

    // For exceptions, you already throw in controllers/services
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiError> handleResponseStatus(ResponseStatusException ex, HttpServletRequest req) {
        int status = ex.getStatusCode().value();
        String error = ex.getStatusCode().toString();

        ApiError body = new ApiError(
                Instant.now().toString(),
                status,
                error,
                ex.getReason(),
                req.getRequestURI()
        );
        return ResponseEntity.status(status).body(body);
    }

    // Bad JSON / missing body, etc.
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleBadJson(HttpMessageNotReadableException ex, HttpServletRequest req) {
        ApiError body = new ApiError(
                Instant.now().toString(),
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.toString(),
                "Invalid JSON request body",
                req.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    // Database constraint violations not caught elsewhere
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrity(DataIntegrityViolationException ex, HttpServletRequest req) {
        ApiError body = new ApiError(
                Instant.now().toString(),
                HttpStatus.CONFLICT.value(),
                HttpStatus.CONFLICT.toString(),
                "Data conflict (possibly duplicate username)",
                req.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    // Fallback for unexpected errors
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleAny(Exception ex, HttpServletRequest req) {
        ApiError body = new ApiError(
                Instant.now().toString(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                HttpStatus.INTERNAL_SERVER_ERROR.toString(),
                "Internal server error",
                req.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}

record ApiError(String timestamp, int status, String error, String message, String path) {}