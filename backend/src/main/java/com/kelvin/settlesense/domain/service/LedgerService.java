package com.kelvin.settlesense.domain.service;

import java.time.Instant;
import java.util.List;

import com.kelvin.settlesense.domain.model.Expense;
import com.kelvin.settlesense.domain.model.ExpenseSplit;
import com.kelvin.settlesense.domain.model.LedgerDirection;
import com.kelvin.settlesense.domain.model.LedgerEntry;
import com.kelvin.settlesense.domain.model.LedgerSourceType;
import com.kelvin.settlesense.domain.model.Settlement;
import org.springframework.stereotype.Service;

@Service
public class LedgerService {

	public List<LedgerEntry> entriesForExpense(Expense expense, List<ExpenseSplit> splits, Instant createdAt) {
		if (expense == null) {
			throw new IllegalArgumentException("expense is required");
		}
		if (splits == null || splits.isEmpty()) {
			throw new IllegalArgumentException("expense splits are required");
		}
		MoneyRules.requirePositive(expense.getTotalMinor(), "expense totalMinor");
		var currencyCode = MoneyRules.normalizeCurrencyCode(expense.getCurrencyCode());

		var splitTotal = splits.stream()
				.peek(split -> MoneyRules.requireSameCurrency(currencyCode, split.getCurrencyCode()))
				.mapToLong(ExpenseSplit::getAmountMinor)
				.sum();
		if (splitTotal != expense.getTotalMinor()) {
			throw new IllegalArgumentException("expense split amounts must sum to expense totalMinor");
		}

		return splits.stream()
				.filter(split -> !split.getOwedByUserId().equals(expense.getPaidByUserId()))
				.filter(split -> split.getAmountMinor() > 0)
				.map(split -> LedgerEntry.of(expense.getGroupId(), LedgerSourceType.EXPENSE, expense.getId(),
						split.getOwedByUserId(), expense.getPaidByUserId(), currencyCode, split.getAmountMinor(),
						LedgerDirection.OWES, createdAt))
				.toList();
	}

	public LedgerEntry entryForSettlement(Settlement settlement, Instant createdAt) {
		if (settlement == null) {
			throw new IllegalArgumentException("settlement is required");
		}
		MoneyRules.requirePositive(settlement.getAmountMinor(), "settlement amountMinor");
		if (settlement.getFromUserId().equals(settlement.getToUserId())) {
			throw new IllegalArgumentException("settlement participants must be different");
		}
		return LedgerEntry.of(settlement.getGroupId(), LedgerSourceType.SETTLEMENT, settlement.getId(),
				settlement.getFromUserId(), settlement.getToUserId(),
				MoneyRules.normalizeCurrencyCode(settlement.getCurrencyCode()), settlement.getAmountMinor(),
				LedgerDirection.PAID, createdAt);
	}

	public List<LedgerEntry> reversalEntries(List<LedgerEntry> originalEntries, Long reversalSourceId,
			Instant createdAt) {
		if (originalEntries == null || originalEntries.isEmpty()) {
			throw new IllegalArgumentException("original ledger entries are required");
		}
		return originalEntries.stream()
				.map(entry -> LedgerEntry.of(entry.getGroupId(), LedgerSourceType.REVERSAL, reversalSourceId,
						entry.getFromUserId(), entry.getToUserId(), entry.getCurrencyCode(), entry.getAmountMinor(),
						oppositeDirection(entry.getDirection()), createdAt))
				.toList();
	}

	private LedgerDirection oppositeDirection(LedgerDirection direction) {
		return switch (direction) {
			case OWES -> LedgerDirection.PAID;
			case PAID -> LedgerDirection.OWES;
		};
	}
}
