package com.kelvin.settlesense.domain.service;

import java.time.LocalDate;

public record PostSettlementCommand(
		Long groupId,
		Long fromUserId,
		Long toUserId,
		long amountMinor,
		LocalDate settlementDate,
		Long createdByUserId) {
}
