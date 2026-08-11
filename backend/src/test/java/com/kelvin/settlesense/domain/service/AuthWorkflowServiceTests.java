package com.kelvin.settlesense.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.kelvin.settlesense.domain.model.User;
import com.kelvin.settlesense.domain.repository.UserRepository;
import com.kelvin.settlesense.security.JwtService;

class AuthWorkflowServiceTests {

    private JwtService jwtService;
    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private com.kelvin.settlesense.security.RefreshTokenService refreshTokenService;
    private AuthWorkflowService authService;

    @BeforeEach
    void setUp() {
        jwtService = mock(JwtService.class);
        userRepository = mock(UserRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        refreshTokenService = mock(com.kelvin.settlesense.security.RefreshTokenService.class);
        when(refreshTokenService.createRefreshToken(any(), any())).thenReturn("refresh-token");
        authService = new AuthWorkflowService(jwtService, userRepository, passwordEncoder, refreshTokenService);
    }


    @Test
    void authenticateReturnsAuthResponseForValidCredentials() {
        // Given
        String email = "test@example.com";
        String password = "password123";
        String hashedPassword = "$2a$12$hashedpassword";

        User user = new User();
        user.setId(100L);
        user.setEmail(email);
        user.setDisplayName("Test User");
        user.setPasswordHash(hashedPassword);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(password, hashedPassword)).thenReturn(true);
        when(jwtService.generateTokenWithDisplayName(any(HashMap.class), eq(user)))
                .thenReturn("jwt-token");

        // When
        var response = authService.authenticate(email, password);

        // Then
        assertThat(response.token()).isEqualTo("jwt-token");
        assertThat(response.userId()).isEqualTo(100L);
        assertThat(response.email()).isEqualTo(email);
        assertThat(response.displayName()).isEqualTo("Test User");
    }

    @Test
    void generateTokenReturnsTokenForValidCredentials() {
        // Given
        String email = "test@example.com";
        String password = "password123";
        String hashedPassword = "$2a$12$hashedpassword";

        User user = new User();
        user.setId(1L);
        user.setEmail(email);
        user.setDisplayName("Test User");
        user.setPasswordHash(hashedPassword);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(password, hashedPassword)).thenReturn(true);
        when(jwtService.generateTokenWithDisplayName(any(HashMap.class), eq(user)))
                .thenReturn("jwt-token");

        // When
        String token = authService.generateToken(email, password);

        // Then
        assertThat(token).isEqualTo("jwt-token");
    }

    @Test
    void generateTokenThrowsExceptionForInvalidEmail() {
        // Given
        String email = "nonexistent@example.com";
        String password = "password123";

        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> authService.generateToken(email, password))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Credentials did not match!");
    }

    @Test
    void generateTokenThrowsExceptionForInvalidPassword() {
        // Given
        String email = "test@example.com";
        String password = "wrongpassword";
        String correctHash = "$2a$12$correcthash";

        User user = new User();
        user.setId(1L);
        user.setEmail(email);
        user.setDisplayName("Test User");
        user.setPasswordHash(correctHash);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(password, correctHash)).thenReturn(false);

        // When/Then
        assertThatThrownBy(() -> authService.generateToken(email, password))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Credentials did not match!");
    }
}
