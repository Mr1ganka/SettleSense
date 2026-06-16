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
@Table(name = "balance_projection")
public class BalanceProjection {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "group_id", nullable = false)
	private Long groupId;

	@Column(name = "from_user_id", nullable = false)
	private Long fromUserId;

	@Column(name = "to_user_id", nullable = false)
	private Long toUserId;

	@Column(name = "currency_code", nullable = false, length = 3)
	private String currencyCode;

	@Column(name = "amount_minor", nullable = false)
	private long amountMinor;

	@Column(name = "computed_at", nullable = false)
	private Instant computedAt;

	public static BalanceProjection of(Long groupId, Long fromUserId, Long toUserId, String currencyCode,
			long amountMinor, Instant computedAt) {
		var projection = new BalanceProjection();
		projection.setGroupId(groupId);
		projection.setFromUserId(fromUserId);
		projection.setToUserId(toUserId);
		projection.setCurrencyCode(currencyCode);
		projection.setAmountMinor(amountMinor);
		projection.setComputedAt(computedAt);
		return projection;
	}
}
