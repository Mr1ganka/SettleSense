package com.kelvin.settlesense.domain.model.dto;

import java.time.Instant;

public record FriendshipResponse(
		Long id,
		UserResponse requester,
		UserResponse addressee,
		String status,
		Instant createdAt,
		Instant updatedAt
) {}
