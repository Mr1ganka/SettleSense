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
		return entriesForMultiPayerExpense(expense, java.util.Map.of(expense.getPaidByUserId(), expense.getTotalMinor()), splits, createdAt);
	}

	public List<LedgerEntry> entriesForMultiPayerExpense(Expense expense, java.util.Map<Long, Long> payerInputs, List<ExpenseSplit> splits, Instant createdAt) {
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

		java.util.Map<Long, Long> actualPayers = (payerInputs != null && !payerInputs.isEmpty())
				? payerInputs
				: java.util.Map.of(expense.getPaidByUserId(), expense.getTotalMinor());

		var paidTotal = actualPayers.values().stream().mapToLong(Long::longValue).sum();
		if (paidTotal != expense.getTotalMinor()) {
			throw new IllegalArgumentException("payer amounts must sum to expense totalMinor");
		}

		java.util.Map<Long, Long> netPositions = new java.util.HashMap<>();
		actualPayers.forEach((userId, paidAmount) ->
				netPositions.put(userId, netPositions.getOrDefault(userId, 0L) + paidAmount));

		for (ExpenseSplit split : splits) {
			netPositions.put(split.getOwedByUserId(),
					netPositions.getOrDefault(split.getOwedByUserId(), 0L) - split.getAmountMinor());
		}

		java.util.List<UserBalance> debtors = new java.util.ArrayList<>();
		java.util.List<UserBalance> creditors = new java.util.ArrayList<>();

		for (var entry : netPositions.entrySet()) {
			long net = entry.getValue();
			if (net < 0) {
				debtors.add(new UserBalance(entry.getKey(), -net));
			} else if (net > 0) {
				creditors.add(new UserBalance(entry.getKey(), net));
			}
		}

		java.util.List<LedgerEntry> entries = new java.util.ArrayList<>();
		int debtorIdx = 0;
		int creditorIdx = 0;

		while (debtorIdx < debtors.size() && creditorIdx < creditors.size()) {
			var debtor = debtors.get(debtorIdx);
			var creditor = creditors.get(creditorIdx);

			long settledAmount = Math.min(debtor.amount, creditor.amount);
			if (settledAmount > 0) {
				entries.add(LedgerEntry.of(expense.getGroupId(), LedgerSourceType.EXPENSE, expense.getId(),
						debtor.userId, creditor.userId, currencyCode, settledAmount,
						LedgerDirection.OWES, createdAt));
			}

			debtor.amount -= settledAmount;
			creditor.amount -= settledAmount;

			if (debtor.amount == 0) debtorIdx++;
			if (creditor.amount == 0) creditorIdx++;
		}

		return entries;
	}

	private static class UserBalance {
		final Long userId;
		long amount;

		UserBalance(Long userId, long amount) {
			this.userId = userId;
			this.amount = amount;
		}
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
