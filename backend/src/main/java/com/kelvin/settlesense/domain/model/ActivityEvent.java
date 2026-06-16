package com.kelvin.settlesense.domain.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "activity_event")
public class ActivityEvent {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "group_id", nullable = false)
	private Long groupId;

	@Column(name = "actor_user_id", nullable = false)
	private Long actorUserId;

	@Column(name = "event_type", nullable = false, length = 80)
	private String eventType;

	@Column(name = "entity_type", nullable = false, length = 80)
	private String entityType;

	@Column(name = "entity_id", nullable = false)
	private Long entityId;

	@Column(name = "message", nullable = false, length = 500)
	private String message;

	@Column(name = "metadata", nullable = false, columnDefinition = "TEXT")
	private String metadata;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;
}
