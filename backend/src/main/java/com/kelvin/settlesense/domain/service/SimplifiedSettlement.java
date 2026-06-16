package com.kelvin.settlesense.domain.service;

public record SimplifiedSettlement(
		Long fromUserId,
		Long toUserId,
		String currencyCode,
		long amountMinor) {
}
