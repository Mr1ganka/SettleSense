package com.kelvin.settlesense.domain.model.dto;

import com.kelvin.settlesense.domain.service.PostSettlementCommand;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;

public record PostSettlementRequest(
		@NotNull Long fromUserId,
		@NotNull Long toUserId,
		@Positive long amountMinor,
		@NotNull LocalDate settlementDate,
		@NotNull Long createdByUserId) {

	public PostSettlementCommand toCommand(Long groupId, Long actorUserId) {
		return new PostSettlementCommand(groupId, fromUserId, toUserId, amountMinor, settlementDate, actorUserId);
	}
}
