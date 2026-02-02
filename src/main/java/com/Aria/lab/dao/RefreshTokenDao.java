package com.Aria.lab.dao;

import com.Aria.lab.model.RefreshToken;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Repository
public class RefreshTokenDao {

    private final JdbcTemplate jdbcTemplate;

    public RefreshTokenDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private static final RowMapper<RefreshToken> ROW_MAPPER = (rs, rowNum) -> {
        RefreshToken t = new RefreshToken();
        t.setId(rs.getInt("id"));
        t.setUserId(rs.getInt("user_id"));
        t.setToken(rs.getString("token"));
        t.setExpiresAt(rs.getTimestamp("expires_at").toInstant());
        t.setRevoked(rs.getBoolean("revoked"));
        Timestamp created = rs.getTimestamp("created_at");
        if (created != null) t.setCreatedAt(created.toInstant());
        return t;
    };

    /** insert: 保存 refresh token，返回带 id 的对象 */
    public RefreshToken insert(int userId, String token, Instant expiresAt) {
        String sql = """
            INSERT INTO refresh_tokens (user_id, token, expires_at, revoked)
            VALUES (?, ?, ?, FALSE)
            """;

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, new String[]{"id"});
            ps.setInt(1, userId);
            ps.setString(2, token);
            ps.setTimestamp(3, Timestamp.from(expiresAt));
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("Failed to get generated key for refresh_tokens.id");
        }

        RefreshToken t = new RefreshToken();
        t.setId(key.intValue());
        t.setUserId(userId);
        t.setToken(token);
        t.setExpiresAt(expiresAt);
        t.setRevoked(false);
        t.setCreatedAt(Instant.now()); // 近似即可；想严格就再查一次 DB
        return t;
    }

    /** find: 按 token 查（只允许未 revoked 且未过期的） */
    public Optional<RefreshToken> findValidByToken(String token, Instant now) {
        String sql = """
                SELECT id, user_id, token, expires_at, revoked, created_at
                FROM refresh_tokens
                WHERE token = ?
                  AND revoked = FALSE
                  AND expires_at > ?
                """;

        List<RefreshToken> list = jdbcTemplate.query(sql, ROW_MAPPER, token, Timestamp.from(now));
        return list.stream().findFirst();
    }
    /** consume: 原子地“消费”一个 refresh token（检查未 revoked + 未过期，同时撤销它）
     *  成功返回 userId；失败返回 empty（表示无效/已用/过期）
     */
    public Optional<Integer> consumeValidToken(String token, Instant now) {
        String sql = """
        UPDATE refresh_tokens
        SET revoked = TRUE
        WHERE token = ?
          AND revoked = FALSE
          AND expires_at > ?
        """;

        int updated = jdbcTemplate.update(sql, token, Timestamp.from(now));
        if (updated == 0) {
            return Optional.empty();
        }

        // 这里安全：只有 update 成功的那一个线程会走到这里
        Integer userId = jdbcTemplate.queryForObject(
                "SELECT user_id FROM refresh_tokens WHERE token = ?",
                Integer.class,
                token
        );
        return Optional.ofNullable(userId);
    }

    /** revoke: 让这个 refresh token 失效（logout 用） */
    public boolean revokeByToken(String token) {
        String sql = "UPDATE refresh_tokens SET revoked = TRUE WHERE token = ? AND revoked = FALSE";
        return jdbcTemplate.update(sql, token) > 0;
    }

    /** deleteExpired: 清理过期 token（可做定时任务/启动时清一次） */
    public int deleteExpired(Instant now) {
        String sql = "DELETE FROM refresh_tokens WHERE expires_at <= ?";
        return jdbcTemplate.update(sql, Timestamp.from(now));
    }
}