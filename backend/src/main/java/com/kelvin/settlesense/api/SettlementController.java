package com.kelvin.settlesense.api;

import java.time.LocalDate;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.kelvin.settlesense.domain.model.Settlement;
import com.kelvin.settlesense.domain.model.dto.CancelMoneyActionRequest;
import com.kelvin.settlesense.domain.model.dto.PostSettlementRequest;
import com.kelvin.settlesense.domain.model.dto.SettlementResponse;
import com.kelvin.settlesense.domain.repository.SettlementRepository;
import com.kelvin.settlesense.domain.service.SettlementWorkflowService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api")
class SettlementController {

	private final SettlementWorkflowService settlementWorkflowService;
	private final SettlementRepository settlementRepository;

	SettlementController(SettlementWorkflowService settlementWorkflowService, SettlementRepository settlementRepository) {
		this.settlementWorkflowService = settlementWorkflowService;
		this.settlementRepository = settlementRepository;
	}

	@GetMapping("/groups/{groupId}/settlements")
	List<SettlementResponse> listSettlements(@PathVariable Long groupId) {
		return settlementRepository.findByGroupIdOrderByIdDesc(groupId).stream()
				.map(SettlementResponse::from)
				.toList();
	}

	@PostMapping("/groups/{groupId}/settlements")
	@ResponseStatus(HttpStatus.CREATED)
	SettlementResponse postSettlement(@PathVariable Long groupId, @Valid @RequestBody PostSettlementRequest request) {
		var settlement = settlementWorkflowService.postSettlement(
				request.toCommand(groupId, currentUserId(request.createdByUserId())));
		return SettlementResponse.from(settlement);
	}

	@PostMapping("/settlements/{settlementId}/cancel")
	SettlementResponse cancelSettlement(@PathVariable Long settlementId,
			@Valid @RequestBody CancelMoneyActionRequest request) {
		var settlement = settlementWorkflowService.cancelSettlement(settlementId, currentUserId(request.actorUserId()),
				request.reason());
		return SettlementResponse.from(settlement);
	}

	private Long currentUserId(Long fallbackUserId) {
		var authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication != null && authentication.getPrincipal() instanceof com.kelvin.settlesense.domain.model.User user
				&& user.getId() != null) {
			return user.getId();
		}
		return fallbackUserId;
	}
}
