package com.kelvin.settlesense.domain.model.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateUserRequest(@NotBlank String displayName) {
}
