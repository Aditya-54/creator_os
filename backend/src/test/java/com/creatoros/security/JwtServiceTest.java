package com.creatoros.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private final JwtService jwtService = new JwtService(
            new JwtProperties("unit-test-secret-key-must-be-32-bytes-min", 60));

    @Test
    void generatesTokenThatRoundTripsUserIdAndEmail() {
        UUID userId = UUID.randomUUID();
        String token = jwtService.generateAccessToken(userId, "creator@example.com");

        var claims = jwtService.parseClaims(token);

        assertThat(jwtService.extractUserId(claims)).isEqualTo(userId);
        assertThat(claims.get("email", String.class)).isEqualTo("creator@example.com");
    }

    @Test
    void rejectsTokenSignedWithDifferentSecret() {
        var otherService = new JwtService(new JwtProperties("a-completely-different-secret-key-32bytes", 60));
        String token = otherService.generateAccessToken(UUID.randomUUID(), "x@example.com");

        assertThatThrownBy(() -> jwtService.parseClaims(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void rejectsExpiredToken() {
        var expiringNowService = new JwtService(new JwtProperties("unit-test-secret-key-must-be-32-bytes-min", 0));
        String token = expiringNowService.generateAccessToken(UUID.randomUUID(), "x@example.com");

        assertThatThrownBy(() -> jwtService.parseClaims(token)).isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void rejectsMalformedToken() {
        assertThatThrownBy(() -> jwtService.parseClaims("not-a-jwt")).isInstanceOf(JwtException.class);
    }

    @Test
    void constructorRejectsSecretShorterThan32Bytes() {
        assertThatThrownBy(() -> new JwtService(new JwtProperties("too-short", 60)))
                .isInstanceOf(IllegalStateException.class);
    }
}
