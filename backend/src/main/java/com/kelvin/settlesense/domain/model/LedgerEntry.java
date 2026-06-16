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
@Table(name = "ledger_entry")
public class LedgerEntry {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "group_id", nullable = false)
	private Long groupId;

	@Enumerated(EnumType.STRING)
	@Column(name = "source_type", nullable = false, length = 40)
	private LedgerSourceType sourceType;

	@Column(name = "source_id", nullable = false)
	private Long sourceId;

	@Column(name = "from_user_id", nullable = false)
	private Long fromUserId;

	@Column(name = "to_user_id", nullable = false)
	private Long toUserId;

	@Column(name = "currency_code", nullable = false, length = 3)
	private String currencyCode;

	@Column(name = "amount_minor", nullable = false)
	private long amountMinor;

	@Enumerated(EnumType.STRING)
	@Column(name = "direction", nullable = false, length = 40)
	private LedgerDirection direction;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	public static LedgerEntry of(Long groupId, LedgerSourceType sourceType, Long sourceId, Long fromUserId,
			Long toUserId, String currencyCode, long amountMinor, LedgerDirection direction, Instant createdAt) {
		var entry = new LedgerEntry();
		entry.setGroupId(groupId);
		entry.setSourceType(sourceType);
		entry.setSourceId(sourceId);
		entry.setFromUserId(fromUserId);
		entry.setToUserId(toUserId);
		entry.setCurrencyCode(currencyCode);
		entry.setAmountMinor(amountMinor);
		entry.setDirection(direction);
		entry.setCreatedAt(createdAt);
		return entry;
	}
}
