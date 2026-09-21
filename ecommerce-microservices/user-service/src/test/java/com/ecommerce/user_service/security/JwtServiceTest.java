package com.ecommerce.user_service.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;
    private UserDetails user;

    @BeforeEach
    void setUp() {
        // same hex key as application.yml
        jwtService = new JwtService(
            "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970",
            900_000L,       // 15 min
            604_800_000L    // 7 days
        );
        user = new User("john", "password",
                List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")));
    }

    @Test
    void accessToken_isValid_forSameUser() {
        String token = jwtService.generateAccessToken(user);

        assertThat(jwtService.isTokenValid(token, user)).isTrue();
        assertThat(jwtService.extractUsername(token)).isEqualTo("john");
    }

    @Test
    void refreshToken_isMarkedAsRefreshType() {
        String token = jwtService.generateRefreshToken(user);

        assertThat(jwtService.isRefreshToken(token)).isTrue();
        assertThat(jwtService.isTokenValid(token, user)).isTrue();
    }

    @Test
    void accessToken_isNotRefreshToken() {
        String token = jwtService.generateAccessToken(user);

        assertThat(jwtService.isRefreshToken(token)).isFalse();
    }

    @Test
    void token_isInvalid_forDifferentUser() {
        String token = jwtService.generateAccessToken(user);
        UserDetails other = new User("alice", "password", List.of());

        assertThat(jwtService.isTokenValid(token, other)).isFalse();
    }

    @Test
    void expiredToken_throwsException() {
        // create service with 1ms expiration
        JwtService shortLived = new JwtService(
            "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970",
            1L, 1L
        );
        String token = shortLived.generateAccessToken(user);

        // wait for expiry
        try { Thread.sleep(10); } catch (InterruptedException ignored) {}

        assertThatThrownBy(() -> shortLived.isTokenValid(token, user))
            .isInstanceOf(Exception.class);
    }
}
