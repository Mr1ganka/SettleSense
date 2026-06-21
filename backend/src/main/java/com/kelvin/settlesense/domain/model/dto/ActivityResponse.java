package com.kelvin.settlesense.domain.model.dto;

import com.kelvin.settlesense.domain.model.ActivityEvent;

import java.time.Instant;

public record ActivityResponse(
		Long id,
		Long groupId,
		Long actorUserId,
		String eventType,
		String entityType,
		Long entityId,
		String message,
		Instant createdAt) {

	public static ActivityResponse from(ActivityEvent event) {
		return new ActivityResponse(
				event.getId(),
				event.getGroupId(),
				event.getActorUserId(),
				event.getEventType(),
				event.getEntityType(),
				event.getEntityId(),
				event.getMessage(),
				event.getCreatedAt());
	}
}
