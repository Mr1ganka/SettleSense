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
@Table(name = "settle_group")
public class Group {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "name", nullable = false, length = 160)
	private String name;

	@Column(name = "currency_code", nullable = false, length = 3)
	private String currencyCode;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 40)
	private GroupStatus status = GroupStatus.ACTIVE;

	@Column(name = "created_by_user_id", nullable = false)
	private Long createdByUserId;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;
}
