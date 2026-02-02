package com.Aria.lab.security;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

public class JwtService {
    private final byte[] secret;
    private final long ttlSeconds;

    public JwtService(
            @Value("${security.jwt.secret}") String secret,
            @Value("${security.jwt.ttl-seconds:3600}") long ttlSeconds
    ) {
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        this.ttlSeconds = ttlSeconds;
    }

    public String issueToken(int userId, String username) {
        String headerJson = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
        long exp = Instant.now().getEpochSecond() + ttlSeconds;

        String payloadJson =
                "{\"sub\":\"" + userId + "\"," +
                        "\"username\":\"" + escapeJson(username) + "\"," +
                        "\"exp\":" + exp + "}";

        String header = b64url(headerJson.getBytes(StandardCharsets.UTF_8));
        String payload = b64url(payloadJson.getBytes(StandardCharsets.UTF_8));
        String signingInput = header + "." + payload;

        String sig = hmacSha256(signingInput);
        return signingInput + "." + sig;
    }

    public JwtClaims verify(String token) {
        String[] parts = token.split("\\.");
        if (parts.length != 3) throw new IllegalArgumentException("bad jwt format");

        String signingInput = parts[0] + "." + parts[1];
        String expectedSig = hmacSha256(signingInput);
        if (!constantTimeEquals(expectedSig, parts[2])) {
            throw new IllegalArgumentException("bad jwt signature");
        }

        String payloadJson = new String(b64urlDecode(parts[1]), StandardCharsets.UTF_8);

        String sub = readJsonString(payloadJson, "sub");
        String username = readJsonString(payloadJson, "username");
        long exp = readJsonLong(payloadJson, "exp");

        if (exp <= Instant.now().getEpochSecond()) {
            throw new IllegalArgumentException("jwt expired");
        }

        int userId;
        try {
            userId = Integer.parseInt(sub);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("bad sub");
        }

        return new JwtClaims(userId, username, exp);
    }
    public String getUsername(String token) {
        return verify(token).username();
    }

    private String hmacSha256(String signingInput) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            byte[] sig = mac.doFinal(signingInput.getBytes(StandardCharsets.UTF_8));
            return b64url(sig);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String b64url(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static byte[] b64urlDecode(String s) {
        return Base64.getUrlDecoder().decode(s);
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) return false;
        byte[] x = a.getBytes(StandardCharsets.UTF_8);
        byte[] y = b.getBytes(StandardCharsets.UTF_8);
        if (x.length != y.length) return false;
        int r = 0;
        for (int i = 0; i < x.length; i++) r |= (x[i] ^ y[i]);
        return r == 0;
    }

    private static String escapeJson(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String readJsonString(String json, String key) {
        String needle = "\"" + key + "\":";
        int i = json.indexOf(needle);
        if (i < 0) throw new IllegalArgumentException("missing claim: " + key);
        int start = json.indexOf('"', i + needle.length());
        int end = json.indexOf('"', start + 1);
        if (start < 0 || end < 0) throw new IllegalArgumentException("bad claim: " + key);
        return json.substring(start + 1, end);
    }

    private static long readJsonLong(String json, String key) {
        String needle = "\"" + key + "\":";
        int i = json.indexOf(needle);
        if (i < 0) throw new IllegalArgumentException("missing claim: " + key);
        int start = i + needle.length();
        while (start < json.length() && Character.isWhitespace(json.charAt(start))) start++;
        int end = start;
        while (end < json.length() && Character.isDigit(json.charAt(end))) end++;
        return Long.parseLong(json.substring(start, end));
    }

    public record JwtClaims(int userId, String username, long exp) {}
}