package com.Aria.lab.dao;

import com.Aria.lab.model.User;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Objects;
import java.util.List;
import java.util.Optional;

@Repository
public class UserDao {

    private final JdbcTemplate jdbcTemplate;

    public UserDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // 把一行数据 -> User 对象
    private static final RowMapper<User> USER_ROW_MAPPER = (rs, rowNum) -> {
        User u = new User();
        u.setId(rs.getInt("id"));
        u.setUsername(rs.getString("username"));
        u.setPassword(rs.getString("password"));
        return u;
    };

    public List<User> findAll() {
        String sql = "SELECT id, username, password FROM users ORDER BY id";
        return jdbcTemplate.query(sql, USER_ROW_MAPPER);
    }
    public Optional<User> findById(int id) {
        String sql = "SELECT id, username, password FROM users WHERE id = ?";

        List<User> result = jdbcTemplate.query(sql, USER_ROW_MAPPER, id);

        return result.stream().findFirst();
    }
    public Optional<User> findByUsername(String username) {
        String sql = "SELECT id, username, password FROM users WHERE username = ?";

        List<User> result = jdbcTemplate.query(sql, USER_ROW_MAPPER, username);

        return result.stream().findFirst();
    }
    public User create(String username, String password) {
        String sql = "INSERT INTO users (username, password) VALUES (?, ?)";

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, username);
            ps.setString(2, password);
            return ps;
        }, keyHolder);

        int newId = Objects.requireNonNull(keyHolder.getKey()).intValue();

        User u = new User();
        u.setId(newId);
        u.setUsername(username);
        u.setPassword(password); // 注意：只是存库用，Controller 不会返回它
        return u;
    }
    public boolean deleteById(int id) {
        String sql = "DELETE FROM users WHERE id = ?";
        int affected = jdbcTemplate.update(sql, id);
        return affected > 0;
    }

    public boolean updateById(int id, String username, String password) {
        String sql = "UPDATE users SET username = ?, password = ? WHERE id = ?";
        int affected = jdbcTemplate.update(sql, username, password, id);
        return affected > 0;
    }
}