package com.kelvin.settlesense.domain.model.dto;

public record AuthResponse(
        String token,
        String refreshToken,
        Long userId,
        String email,
        String displayName
) {
}

