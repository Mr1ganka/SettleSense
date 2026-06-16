package com.kelvin.settlesense.domain.service;

import java.time.Clock;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.kelvin.settlesense.domain.model.BalanceProjection;
import com.kelvin.settlesense.domain.model.LedgerDirection;
import com.kelvin.settlesense.domain.model.LedgerEntry;
import org.springframework.stereotype.Service;

@Service
public class BalanceProjectionService {

	public List<BalanceProjection> rebuild(Long groupId, String currencyCode, List<LedgerEntry> entries, Clock clock) {
		var normalizedCurrency = MoneyRules.normalizeCurrencyCode(currencyCode);
		var pairNets = new HashMap<PairKey, Long>();

		for (var entry : entries) {
			if (!entry.getGroupId().equals(groupId)) {
				continue;
			}
			MoneyRules.requireSameCurrency(normalizedCurrency, entry.getCurrencyCode());
			MoneyRules.requirePositive(entry.getAmountMinor(), "ledger amountMinor");
			applyEntry(pairNets, entry);
		}

		var computedAt = clock.instant();
		return pairNets.entrySet().stream()
				.filter(entry -> entry.getValue() != 0)
				.map(entry -> {
					var key = entry.getKey();
					var amount = entry.getValue();
					if (amount > 0) {
						return BalanceProjection.of(groupId, key.lowUserId(), key.highUserId(), normalizedCurrency, amount,
								computedAt);
					}
					return BalanceProjection.of(groupId, key.highUserId(), key.lowUserId(), normalizedCurrency,
							Math.abs(amount), computedAt);
				})
				.sorted(Comparator.comparing(BalanceProjection::getFromUserId)
						.thenComparing(BalanceProjection::getToUserId))
				.toList();
	}

	public List<SimplifiedSettlement> suggestSimplifiedSettlements(List<BalanceProjection> projections) {
		if (projections == null || projections.isEmpty()) {
			return List.of();
		}

		var currencyCode = MoneyRules.normalizeCurrencyCode(projections.getFirst().getCurrencyCode());
		var netByUser = new HashMap<Long, Long>();
		for (var projection : projections) {
			MoneyRules.requireSameCurrency(currencyCode, projection.getCurrencyCode());
			MoneyRules.requirePositive(projection.getAmountMinor(), "projection amountMinor");
			netByUser.merge(projection.getFromUserId(), -projection.getAmountMinor(), Long::sum);
			netByUser.merge(projection.getToUserId(), projection.getAmountMinor(), Long::sum);
		}

		var debtors = netByUser.entrySet().stream()
				.filter(entry -> entry.getValue() < 0)
				.map(entry -> new Position(entry.getKey(), -entry.getValue()))
				.sorted(Position.byAmountDescThenUser())
				.toList();
		var creditors = netByUser.entrySet().stream()
				.filter(entry -> entry.getValue() > 0)
				.map(entry -> new Position(entry.getKey(), entry.getValue()))
				.sorted(Position.byAmountDescThenUser())
				.toList();

		var mutableDebtors = new ArrayList<>(debtors);
		var mutableCreditors = new ArrayList<>(creditors);
		var suggestions = new ArrayList<SimplifiedSettlement>();
		var debtorIndex = 0;
		var creditorIndex = 0;
		while (debtorIndex < mutableDebtors.size() && creditorIndex < mutableCreditors.size()) {
			var debtor = mutableDebtors.get(debtorIndex);
			var creditor = mutableCreditors.get(creditorIndex);
			var amount = Math.min(debtor.amountMinor(), creditor.amountMinor());
			suggestions.add(new SimplifiedSettlement(debtor.userId(), creditor.userId(), currencyCode, amount));

			mutableDebtors.set(debtorIndex, debtor.minus(amount));
			mutableCreditors.set(creditorIndex, creditor.minus(amount));
			if (mutableDebtors.get(debtorIndex).amountMinor() == 0) {
				debtorIndex++;
			}
			if (mutableCreditors.get(creditorIndex).amountMinor() == 0) {
				creditorIndex++;
			}
		}
		return suggestions;
	}

	private void applyEntry(Map<PairKey, Long> pairNets, LedgerEntry entry) {
		var key = PairKey.of(entry.getFromUserId(), entry.getToUserId());
		var signedAmount = entry.getDirection() == LedgerDirection.OWES ? entry.getAmountMinor()
				: -entry.getAmountMinor();
		if (!entry.getFromUserId().equals(key.lowUserId())) {
			signedAmount = -signedAmount;
		}
		pairNets.merge(key, signedAmount, Long::sum);
	}

	private record PairKey(Long lowUserId, Long highUserId) {
		static PairKey of(Long firstUserId, Long secondUserId) {
			if (firstUserId.equals(secondUserId)) {
				throw new IllegalArgumentException("balance participants must be different");
			}
			if (firstUserId < secondUserId) {
				return new PairKey(firstUserId, secondUserId);
			}
			return new PairKey(secondUserId, firstUserId);
		}
	}

	private record Position(Long userId, long amountMinor) {
		static Comparator<Position> byAmountDescThenUser() {
			return Comparator.comparingLong(Position::amountMinor).reversed().thenComparing(Position::userId);
		}

		Position minus(long amount) {
			return new Position(userId, amountMinor - amount);
		}
	}
}
