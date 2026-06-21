package com.kelvin.settlesense.domain.model.dto;

import com.kelvin.settlesense.domain.service.SimplifiedSettlement;

public record SimplifiedSettlementResponse(Long fromUserId, Long toUserId, String currencyCode, long amountMinor) {

	public static SimplifiedSettlementResponse from(SimplifiedSettlement settlement) {
		return new SimplifiedSettlementResponse(settlement.fromUserId(), settlement.toUserId(),
				settlement.currencyCode(), settlement.amountMinor());
	}
}
