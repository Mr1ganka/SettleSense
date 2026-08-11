package com.kelvin.settlesense.domain.service;

import com.kelvin.settlesense.domain.model.User;
import com.kelvin.settlesense.domain.model.dto.AuthResponse;
import com.kelvin.settlesense.domain.repository.UserRepository;
import com.kelvin.settlesense.security.JwtService;
import com.kelvin.settlesense.security.RefreshTokenService;
import org.springframework.stereotype.Service;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashMap;
import java.util.Optional;

@Service
public class AuthWorkflowService {

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;

    public AuthWorkflowService(JwtService jwtService, UserRepository userRepository, PasswordEncoder passwordEncoder, RefreshTokenService refreshTokenService) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.refreshTokenService = refreshTokenService;
    }

    public AuthResponse authenticate(String email, String password) {
        Optional<User> user = userRepository.findByEmail(email);
        if (user.isEmpty() || !passwordEncoder.matches(password, user.get().getPasswordHash())) {
            throw new BadCredentialsException("Credentials did not match!");
        }

        User authenticatedUser = user.get();
        return buildAuthResponse(authenticatedUser);
    }

    public AuthResponse refreshToken(String refreshToken) {
        String tokenData = refreshTokenService.validateAndConsumeRefreshToken(refreshToken);
        if (tokenData == null) {
            throw new BadCredentialsException("Invalid or expired refresh token");
        }

        String[] parts = tokenData.split(":");
        Long userId = Long.parseLong(parts[0]);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadCredentialsException("User not found for refresh token"));

        return buildAuthResponse(user);
    }

    public void logout(String refreshToken) {
        refreshTokenService.revokeRefreshToken(refreshToken);
    }

    public String generateToken(String email, String password) {
        return authenticate(email, password).token();
    }

    private AuthResponse buildAuthResponse(User authenticatedUser) {
        HashMap<String, String> claims = new HashMap<>();
        claims.put("email", authenticatedUser.getEmail());
        claims.put("displayName", authenticatedUser.getDisplayName());

        String accessToken = jwtService.generateTokenWithDisplayName(claims, authenticatedUser);
        String refreshToken = refreshTokenService.createRefreshToken(authenticatedUser.getId(), authenticatedUser.getEmail());
        return new AuthResponse(accessToken, refreshToken, authenticatedUser.getId(), authenticatedUser.getEmail(), authenticatedUser.getDisplayName());
    }
}

