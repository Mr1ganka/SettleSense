package com.kelvin.settlesense.domain.model.dto;

public record SendFriendRequest(
		String email,
		Long targetUserId
) {}
