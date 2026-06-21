package com.kelvin.settlesense.domain.model.dto;

import com.kelvin.settlesense.domain.model.BalanceProjection;

public record BalanceResponse(Long fromUserId, Long toUserId, String currencyCode, long amountMinor) {

	public static BalanceResponse from(BalanceProjection projection) {
		return new BalanceResponse(projection.getFromUserId(), projection.getToUserId(), projection.getCurrencyCode(),
				projection.getAmountMinor());
	}
}
