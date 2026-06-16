package com.kelvin.settlesense.domain.service;

import java.time.Instant;

import com.kelvin.settlesense.domain.model.ActivityEvent;

public final class ActivityEventFactory {

	private ActivityEventFactory() {
	}

	public static ActivityEvent event(Long groupId, Long actorUserId, String eventType, String entityType, Long entityId,
			String message, String metadata, Instant createdAt) {
		var event = new ActivityEvent();
		event.setGroupId(groupId);
		event.setActorUserId(actorUserId);
		event.setEventType(eventType);
		event.setEntityType(entityType);
		event.setEntityId(entityId);
		event.setMessage(message);
		event.setMetadata(metadata == null ? "{}" : metadata);
		event.setCreatedAt(createdAt);
		return event;
	}
}
