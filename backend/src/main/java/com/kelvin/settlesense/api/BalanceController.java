package com.kelvin.settlesense.api;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kelvin.settlesense.domain.model.BalanceProjection;
import com.kelvin.settlesense.domain.repository.BalanceProjectionRepository;
import com.kelvin.settlesense.domain.service.BalanceProjectionService;
import com.kelvin.settlesense.domain.service.SimplifiedSettlement;

@RestController
@RequestMapping("/api/groups/{groupId}")
class BalanceController {

	private final BalanceProjectionRepository balanceProjectionRepository;
	private final BalanceProjectionService balanceProjectionService;

	BalanceController(BalanceProjectionRepository balanceProjectionRepository,
			BalanceProjectionService balanceProjectionService) {
		this.balanceProjectionRepository = balanceProjectionRepository;
		this.balanceProjectionService = balanceProjectionService;
	}

	@GetMapping("/balances")
	List<BalanceResponse> balances(@PathVariable Long groupId) {
		return balanceProjectionRepository.findByGroupIdOrderByFromUserIdAscToUserIdAsc(groupId).stream()
				.map(BalanceResponse::from)
				.toList();
	}

	@GetMapping("/settlement-suggestions")
	List<SimplifiedSettlementResponse> settlementSuggestions(@PathVariable Long groupId) {
		var projections = balanceProjectionRepository.findByGroupIdOrderByFromUserIdAscToUserIdAsc(groupId);
		return balanceProjectionService.suggestSimplifiedSettlements(projections).stream()
				.map(SimplifiedSettlementResponse::from)
				.toList();
	}

	record BalanceResponse(Long fromUserId, Long toUserId, String currencyCode, long amountMinor) {
		static BalanceResponse from(BalanceProjection projection) {
			return new BalanceResponse(projection.getFromUserId(), projection.getToUserId(), projection.getCurrencyCode(),
					projection.getAmountMinor());
		}
	}

	record SimplifiedSettlementResponse(Long fromUserId, Long toUserId, String currencyCode, long amountMinor) {
		static SimplifiedSettlementResponse from(SimplifiedSettlement settlement) {
			return new SimplifiedSettlementResponse(settlement.fromUserId(), settlement.toUserId(),
					settlement.currencyCode(), settlement.amountMinor());
		}
	}
}
