package com.kelvin.settlesense.domain.model.dto;

import com.kelvin.settlesense.domain.model.Settlement;

import java.time.LocalDate;

public record SettlementResponse(
		Long id,
		Long groupId,
		Long fromUserId,
		Long toUserId,
		String currencyCode,
		long amountMinor,
		LocalDate settlementDate,
		String status) {

	public static SettlementResponse from(Settlement settlement) {
		return new SettlementResponse(settlement.getId(), settlement.getGroupId(), settlement.getFromUserId(),
				settlement.getToUserId(), settlement.getCurrencyCode(), settlement.getAmountMinor(),
				settlement.getSettlementDate(), settlement.getStatus().name());
	}
}
