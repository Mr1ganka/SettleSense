package com.kelvin.settlesense.domain.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record RegisterUserRequest(
		@NotBlank String displayName,
		@NotBlank @Email String email,
		@NotBlank String password) {

	public RegisterUserDto toCommand() {
		return new RegisterUserDto(displayName, email, password);
	}
}
