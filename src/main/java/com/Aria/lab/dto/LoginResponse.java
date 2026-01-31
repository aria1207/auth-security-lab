package com.Aria.lab.dto;

public record LoginResponse(
        int id,
        String username,
        String accessToken,
        String refreshToken
) {}