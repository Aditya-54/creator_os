package com.creatoros.user;

import static org.assertj.core.api.Assertions.assertThat;

import com.creatoros.support.AbstractIntegrationTest;
import com.creatoros.user.dto.AuthResponse;
import com.creatoros.user.dto.LoginRequest;
import com.creatoros.user.dto.RegisterRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class AuthApiIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void registerLoginAndAccessProtectedEndpoint() {
        RegisterRequest registerRequest = new RegisterRequest("flow@example.com", "password123", "Flow Tester");
        ResponseEntity<AuthResponse> registerResponse =
                restTemplate.postForEntity("/api/auth/register", registerRequest, AuthResponse.class);
        assertThat(registerResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(registerResponse.getBody()).isNotNull();
        assertThat(registerResponse.getBody().accessToken()).isNotBlank();

        ResponseEntity<AuthResponse> loginResponse = restTemplate.postForEntity(
                "/api/auth/login", new LoginRequest("flow@example.com", "password123"), AuthResponse.class);
        assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        String token = loginResponse.getBody().accessToken();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        ResponseEntity<String> meResponse = restTemplate.exchange(
                "/api/auth/me", org.springframework.http.HttpMethod.GET, new HttpEntity<>(headers), String.class);
        assertThat(meResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(meResponse.getBody()).contains("flow@example.com");
    }

    @Test
    void registerRejectsDuplicateEmailWithConflict() {
        RegisterRequest request = new RegisterRequest("dup@example.com", "password123", "Dup");
        restTemplate.postForEntity("/api/auth/register", request, AuthResponse.class);

        ResponseEntity<String> second = restTemplate.postForEntity("/api/auth/register", request, String.class);

        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(second.getBody()).contains("EMAIL_ALREADY_REGISTERED");
    }

    @Test
    void loginWithWrongPasswordReturns401() {
        restTemplate.postForEntity("/api/auth/register",
                new RegisterRequest("bad-login@example.com", "password123", null), AuthResponse.class);

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/auth/login", new LoginRequest("bad-login@example.com", "wrong-password"), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void protectedEndpointWithoutTokenReturns401() {
        ResponseEntity<String> response = restTemplate.getForEntity("/api/auth/me", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void protectedEndpointWithInvalidTokenReturns401() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth("this-is-not-a-valid-jwt");
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/auth/me", org.springframework.http.HttpMethod.GET, new HttpEntity<>(headers), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void registerValidatesInput() {
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/auth/register", new RegisterRequest("not-an-email", "short", null), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("VALIDATION_ERROR");
    }
}
