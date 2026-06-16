package com.kelvin.settlesense.api;

import java.time.LocalDate;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.kelvin.settlesense.domain.model.Settlement;
import com.kelvin.settlesense.domain.service.PostSettlementCommand;
import com.kelvin.settlesense.domain.service.SettlementWorkflowService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@RestController
@RequestMapping("/api")
class SettlementController {

	private final SettlementWorkflowService settlementWorkflowService;

	SettlementController(SettlementWorkflowService settlementWorkflowService) {
		this.settlementWorkflowService = settlementWorkflowService;
	}

	@PostMapping("/groups/{groupId}/settlements")
	@ResponseStatus(HttpStatus.CREATED)
	SettlementResponse postSettlement(@PathVariable Long groupId, @Valid @RequestBody PostSettlementRequest request) {
		var settlement = settlementWorkflowService.postSettlement(request.toCommand(groupId));
		return SettlementResponse.from(settlement);
	}

	@PostMapping("/settlements/{settlementId}/cancel")
	SettlementResponse cancelSettlement(@PathVariable Long settlementId,
			@Valid @RequestBody CancelMoneyActionRequest request) {
		var settlement = settlementWorkflowService.cancelSettlement(settlementId, request.actorUserId(), request.reason());
		return SettlementResponse.from(settlement);
	}

	record PostSettlementRequest(
			@NotNull Long fromUserId,
			@NotNull Long toUserId,
			@Positive long amountMinor,
			@NotNull LocalDate settlementDate,
			@NotNull Long createdByUserId) {

		PostSettlementCommand toCommand(Long groupId) {
			return new PostSettlementCommand(groupId, fromUserId, toUserId, amountMinor, settlementDate, createdByUserId);
		}
	}

	record CancelMoneyActionRequest(@NotNull Long actorUserId, String reason) {
	}

	record SettlementResponse(
			Long id,
			Long groupId,
			Long fromUserId,
			Long toUserId,
			String currencyCode,
			long amountMinor,
			LocalDate settlementDate,
			String status) {

		static SettlementResponse from(Settlement settlement) {
			return new SettlementResponse(settlement.getId(), settlement.getGroupId(), settlement.getFromUserId(),
					settlement.getToUserId(), settlement.getCurrencyCode(), settlement.getAmountMinor(),
					settlement.getSettlementDate(), settlement.getStatus().name());
		}
	}
}
