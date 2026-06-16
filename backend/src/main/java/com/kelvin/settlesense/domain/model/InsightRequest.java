package com.kelvin.settlesense.domain.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "insight_request")
public class InsightRequest {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "group_id", nullable = false)
	private Long groupId;

	@Column(name = "requested_by_user_id", nullable = false)
	private Long requestedByUserId;

	@Column(name = "request_type", nullable = false, length = 80)
	private String requestType;

	@Column(name = "prompt", nullable = false, columnDefinition = "TEXT")
	private String prompt;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 40)
	private InsightRequestStatus status = InsightRequestStatus.PENDING;

	@Column(name = "result", columnDefinition = "TEXT")
	private String result;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "completed_at")
	private Instant completedAt;
}
