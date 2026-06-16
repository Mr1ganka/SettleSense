package com.kelvin.settlesense.domain.model;

import java.time.Instant;
import java.time.LocalDate;

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
@Table(name = "expense")
public class Expense {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "group_id", nullable = false)
	private Long groupId;

	@Column(name = "paid_by_user_id", nullable = false)
	private Long paidByUserId;

	@Column(name = "description", nullable = false, length = 240)
	private String description;

	@Column(name = "currency_code", nullable = false, length = 3)
	private String currencyCode;

	@Column(name = "total_minor", nullable = false)
	private long totalMinor;

	@Column(name = "expense_date", nullable = false)
	private LocalDate expenseDate;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 40)
	private ExpenseStatus status = ExpenseStatus.POSTED;

	@Column(name = "created_by_user_id", nullable = false)
	private Long createdByUserId;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Column(name = "cancelled_at")
	private Instant cancelledAt;

	@Column(name = "cancelled_by_user_id")
	private Long cancelledByUserId;

	@Column(name = "cancellation_reason", length = 500)
	private String cancellationReason;
}
