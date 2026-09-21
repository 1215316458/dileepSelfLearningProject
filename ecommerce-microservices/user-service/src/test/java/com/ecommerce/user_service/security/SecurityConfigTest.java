package com.ecommerce.user_service.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class SecurityConfigTest {

    @Autowired
    PasswordEncoder passwordEncoder;

    @Test
    void passwordEncoder_encodesAndMatchesCorrectly() {
        String raw     = "mySecret123";
        String encoded = passwordEncoder.encode(raw);

        // encoded hash is never equal to raw password
        assertThat(encoded).isNotEqualTo(raw);

        // matches() verifies raw against BCrypt hash
        assertThat(passwordEncoder.matches(raw, encoded)).isTrue();

        // wrong password does not match
        assertThat(passwordEncoder.matches("wrongPassword", encoded)).isFalse();
    }

    @Test
    void passwordEncoder_samePasswordProducesDifferentHashes() {
        // BCrypt auto-salts — same input produces different hashes each time
        String hash1 = passwordEncoder.encode("password");
        String hash2 = passwordEncoder.encode("password");

        assertThat(hash1).isNotEqualTo(hash2);                    // different hashes
        assertThat(passwordEncoder.matches("password", hash1)).isTrue();  // both valid
        assertThat(passwordEncoder.matches("password", hash2)).isTrue();
    }
}
