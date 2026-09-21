package com.ecommerce.user_service.dto;

// Returned after successful login or token refresh
public record AuthResponse(
    String accessToken,
    String refreshToken,
    String tokenType,
    long   expiresIn    // access token TTL in seconds
) {
    public static AuthResponse of(String accessToken, String refreshToken, long expiresInMs) {
        return new AuthResponse(accessToken, refreshToken, "Bearer", expiresInMs / 1000);
    }
}
