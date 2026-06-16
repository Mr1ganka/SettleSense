package com.kelvin.settlesense.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.kelvin.settlesense.domain.model.Expense;
import com.kelvin.settlesense.domain.model.ExpenseSplit;
import com.kelvin.settlesense.domain.model.LedgerDirection;
import com.kelvin.settlesense.domain.model.LedgerEntry;
import com.kelvin.settlesense.domain.model.LedgerSourceType;
import com.kelvin.settlesense.domain.model.Settlement;
import com.kelvin.settlesense.domain.model.SplitType;

class LedgerAndBalanceServiceTests {

	private static final Instant NOW = Instant.parse("2026-06-01T00:00:00Z");
	private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

	private final LedgerService ledgerService = new LedgerService();
	private final BalanceProjectionService balanceService = new BalanceProjectionService();

	@Test
	void expenseLedgerSkipsPayersOwnSplitAndProducesBalance() {
		var expense = expense(100L, 1L, 1L, 100000);
		var splits = List.of(split(100L, 1L, 50000), split(100L, 2L, 50000));

		var entries = ledgerService.entriesForExpense(expense, splits, NOW);
		var balances = balanceService.rebuild(1L, "INR", entries, CLOCK);

		assertThat(entries).hasSize(1);
		assertThat(entries.getFirst().getFromUserId()).isEqualTo(2L);
		assertThat(entries.getFirst().getToUserId()).isEqualTo(1L);
		assertThat(entries.getFirst().getDirection()).isEqualTo(LedgerDirection.OWES);
		assertThat(balances).hasSize(1);
		assertThat(balances.getFirst().getFromUserId()).isEqualTo(2L);
		assertThat(balances.getFirst().getToUserId()).isEqualTo(1L);
		assertThat(balances.getFirst().getAmountMinor()).isEqualTo(50000);
	}

	@Test
	void settlementReducesBalanceAndOverSettlementReversesDirection() {
		var expenseEntry = LedgerEntry.of(1L, LedgerSourceType.EXPENSE, 100L, 2L, 1L, "INR", 50000,
				LedgerDirection.OWES, NOW);
		var settlementEntry = ledgerService.entryForSettlement(settlement(200L, 1L, 2L, 1L, 70000), NOW);

		var balances = balanceService.rebuild(1L, "INR", List.of(expenseEntry, settlementEntry), CLOCK);

		assertThat(balances).hasSize(1);
		assertThat(balances.getFirst().getFromUserId()).isEqualTo(1L);
		assertThat(balances.getFirst().getToUserId()).isEqualTo(2L);
		assertThat(balances.getFirst().getAmountMinor()).isEqualTo(20000);
	}

	@Test
	void reversalEntriesCancelOriginalLedgerImpact() {
		var original = LedgerEntry.of(1L, LedgerSourceType.EXPENSE, 100L, 2L, 1L, "INR", 50000,
				LedgerDirection.OWES, NOW);
		var reversal = ledgerService.reversalEntries(List.of(original), 100L, NOW.plusSeconds(60));

		var balances = balanceService.rebuild(1L, "INR", List.of(original, reversal.getFirst()), CLOCK);

		assertThat(reversal.getFirst().getSourceType()).isEqualTo(LedgerSourceType.REVERSAL);
		assertThat(reversal.getFirst().getDirection()).isEqualTo(LedgerDirection.PAID);
		assertThat(balances).isEmpty();
	}

	@Test
	void simplifiedSettlementsUseNetGroupPositionsWithoutMutatingBalances() {
		var projections = List.of(
				balance(1L, 2L, 30000),
				balance(1L, 3L, 20000));

		var suggestions = balanceService.suggestSimplifiedSettlements(projections);

		assertThat(suggestions).containsExactly(
				new SimplifiedSettlement(1L, 2L, "INR", 30000),
				new SimplifiedSettlement(1L, 3L, "INR", 20000));
	}

	private Expense expense(Long id, Long groupId, Long paidByUserId, long totalMinor) {
		var expense = new Expense();
		expense.setId(id);
		expense.setGroupId(groupId);
		expense.setPaidByUserId(paidByUserId);
		expense.setDescription("Dinner");
		expense.setCurrencyCode("INR");
		expense.setTotalMinor(totalMinor);
		expense.setExpenseDate(LocalDate.parse("2026-06-01"));
		expense.setCreatedByUserId(paidByUserId);
		expense.setCreatedAt(NOW);
		expense.setUpdatedAt(NOW);
		return expense;
	}

	private ExpenseSplit split(Long expenseId, Long owedByUserId, long amountMinor) {
		var split = new ExpenseSplit();
		split.setExpenseId(expenseId);
		split.setOwedByUserId(owedByUserId);
		split.setSplitType(SplitType.EXACT);
		split.setInputValue(BigDecimal.valueOf(amountMinor));
		split.setAmountMinor(amountMinor);
		split.setCurrencyCode("INR");
		split.setCreatedAt(NOW);
		return split;
	}

	private Settlement settlement(Long id, Long groupId, Long fromUserId, Long toUserId, long amountMinor) {
		var settlement = new Settlement();
		settlement.setId(id);
		settlement.setGroupId(groupId);
		settlement.setFromUserId(fromUserId);
		settlement.setToUserId(toUserId);
		settlement.setCurrencyCode("INR");
		settlement.setAmountMinor(amountMinor);
		settlement.setSettlementDate(LocalDate.parse("2026-06-01"));
		settlement.setCreatedByUserId(fromUserId);
		settlement.setCreatedAt(NOW);
		settlement.setUpdatedAt(NOW);
		return settlement;
	}

	private com.kelvin.settlesense.domain.model.BalanceProjection balance(Long fromUserId, Long toUserId,
			long amountMinor) {
		return com.kelvin.settlesense.domain.model.BalanceProjection.of(1L, fromUserId, toUserId, "INR", amountMinor,
				NOW);
	}
}
