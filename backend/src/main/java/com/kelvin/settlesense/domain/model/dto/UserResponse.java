package com.kelvin.settlesense.domain.model.dto;

import com.kelvin.settlesense.domain.model.User;

public record UserResponse(Long id, String displayName, String email, String status) {

	public static UserResponse from(User user) {
		return new UserResponse(user.getId(), user.getDisplayName(), user.getEmail(), user.getStatus().name());
	}
}
