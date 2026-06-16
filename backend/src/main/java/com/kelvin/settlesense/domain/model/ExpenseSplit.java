package com.kelvin.settlesense.domain.model;

import java.math.BigDecimal;
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
@Table(name = "expense_split")
public class ExpenseSplit {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "expense_id", nullable = false)
	private Long expenseId;

	@Column(name = "owed_by_user_id", nullable = false)
	private Long owedByUserId;

	@Enumerated(EnumType.STRING)
	@Column(name = "split_type", nullable = false, length = 40)
	private SplitType splitType;

	@Column(name = "input_value", nullable = false, precision = 19, scale = 6)
	private BigDecimal inputValue;

	@Column(name = "amount_minor", nullable = false)
	private long amountMinor;

	@Column(name = "currency_code", nullable = false, length = 3)
	private String currencyCode;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;
}
