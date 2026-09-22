package com.creatoros.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.creatoros.exception.ConflictException;
import com.creatoros.security.JwtService;
import com.creatoros.user.dto.AuthResponse;
import com.creatoros.user.dto.LoginRequest;
import com.creatoros.user.dto.RegisterRequest;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;

    private AuthService authService() {
        return new AuthService(userRepository, passwordEncoder, jwtService);
    }

    @Test
    void registerRejectsDuplicateEmail() {
        when(userRepository.existsByEmailIgnoreCase("taken@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService().register(
                new RegisterRequest("taken@example.com", "password123", "Name")))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    void registerHashesPasswordAndIssuesToken() {
        when(userRepository.existsByEmailIgnoreCase(any())).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed-password");
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(jwtService.generateAccessToken(any(), any())).thenReturn("signed.jwt.token");
        when(jwtService.accessTokenTtlSeconds()).thenReturn(3600L);

        AuthResponse response = authService().register(
                new RegisterRequest("new@example.com", "password123", "New Creator"));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getPasswordHash()).isEqualTo("hashed-password");
        assertThat(captor.getValue().getEmail()).isEqualTo("new@example.com");
        assertThat(response.accessToken()).isEqualTo("signed.jwt.token");
        assertThat(response.user().email()).isEqualTo("new@example.com");
    }

    @Test
    void loginRejectsUnknownEmail() {
        when(userRepository.findByEmailIgnoreCase("missing@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService().login(new LoginRequest("missing@example.com", "password123")))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void loginRejectsWrongPassword() {
        User user = User.builder().email("user@example.com").passwordHash("hashed").build();
        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> authService().login(new LoginRequest("user@example.com", "wrong")))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void loginSucceedsWithCorrectPassword() {
        User user = User.builder().email("user@example.com").passwordHash("hashed").build();
        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("correct", "hashed")).thenReturn(true);
        when(jwtService.generateAccessToken(any(), any())).thenReturn("signed.jwt.token");
        when(jwtService.accessTokenTtlSeconds()).thenReturn(3600L);

        AuthResponse response = authService().login(new LoginRequest("user@example.com", "correct"));

        assertThat(response.accessToken()).isEqualTo("signed.jwt.token");
    }
}
