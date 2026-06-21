package com.kelvin.settlesense.domain.service;

import java.time.Clock;
import java.util.List;
import java.util.Objects;

import com.kelvin.settlesense.exceptions.ExpenseUpdateException;
import com.kelvin.settlesense.exceptions.GroupUpdateException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kelvin.settlesense.domain.model.Expense;
import com.kelvin.settlesense.domain.model.ExpenseSplit;
import com.kelvin.settlesense.domain.model.ExpenseStatus;
import com.kelvin.settlesense.domain.model.GroupMemberStatus;
import com.kelvin.settlesense.domain.model.GroupStatus;
import com.kelvin.settlesense.domain.model.LedgerSourceType;
import com.kelvin.settlesense.domain.repository.ActivityEventRepository;
import com.kelvin.settlesense.domain.repository.ExpenseRepository;
import com.kelvin.settlesense.domain.repository.ExpenseSplitRepository;
import com.kelvin.settlesense.domain.repository.GroupMemberRepository;
import com.kelvin.settlesense.domain.repository.GroupRepository;
import com.kelvin.settlesense.domain.repository.LedgerEntryRepository;

@Service
public class ExpenseWorkflowService {

	private final GroupRepository groupRepository;
	private final GroupMemberRepository groupMemberRepository;
	private final ExpenseRepository expenseRepository;
	private final ExpenseSplitRepository expenseSplitRepository;
	private final LedgerEntryRepository ledgerEntryRepository;
	private final ActivityEventRepository activityEventRepository;
	private final SplitCalculator splitCalculator;
	private final LedgerService ledgerService;
	private final BalanceProjectionUpdater balanceProjectionUpdater;
	private final Clock clock;

	public ExpenseWorkflowService(GroupRepository groupRepository, GroupMemberRepository groupMemberRepository,
			ExpenseRepository expenseRepository, ExpenseSplitRepository expenseSplitRepository,
			LedgerEntryRepository ledgerEntryRepository, ActivityEventRepository activityEventRepository,
			SplitCalculator splitCalculator, LedgerService ledgerService, BalanceProjectionUpdater balanceProjectionUpdater,
			Clock clock) {
		this.groupRepository = groupRepository;
		this.groupMemberRepository = groupMemberRepository;
		this.expenseRepository = expenseRepository;
		this.expenseSplitRepository = expenseSplitRepository;
		this.ledgerEntryRepository = ledgerEntryRepository;
		this.activityEventRepository = activityEventRepository;
		this.splitCalculator = splitCalculator;
		this.ledgerService = ledgerService;
		this.balanceProjectionUpdater = balanceProjectionUpdater;
		this.clock = clock;
	}

	@Transactional
	public Expense postExpense(PostExpenseCommand command) {
		var now = clock.instant();
		var group = groupRepository.findById(command.groupId())
				.orElseThrow(() -> new GroupUpdateException("group not found"));
		if (group.getStatus() != GroupStatus.ACTIVE) {
			throw new GroupUpdateException(("group must be active"));
		}
		requireActiveMember(command.groupId(), command.paidByUserId(), "payer");
		requireActiveMember(command.groupId(), command.createdByUserId(), "creator");
		command.splitInputsByUserId().keySet()
				.forEach(userId -> requireActiveMember(command.groupId(), userId, "split participant"));

		var currencyCode = MoneyRules.normalizeCurrencyCode(group.getCurrencyCode());
		var expense = new Expense();
		expense.setGroupId(group.getId());
		expense.setPaidByUserId(command.paidByUserId());
		expense.setDescription(command.description());
		expense.setCurrencyCode(currencyCode);
		expense.setTotalMinor(command.totalMinor());
		expense.setExpenseDate(command.expenseDate());
		expense.setStatus(ExpenseStatus.POSTED);
		expense.setCreatedByUserId(command.createdByUserId());
		expense.setCreatedAt(now);
		expense.setUpdatedAt(now);
		var savedExpense = expenseRepository.save(expense);

		var calculatedSplits = splitCalculator.calculate(command.totalMinor(), currencyCode, command.splitType(),
				command.splitInputsByUserId());
		var splitRows = calculatedSplits.stream()
				.map(split -> toExpenseSplit(savedExpense.getId(), split, now))
				.toList();
		var savedSplits = expenseSplitRepository.saveAll(splitRows);
		ledgerEntryRepository.saveAll(ledgerService.entriesForExpense(savedExpense, savedSplits, now));
		activityEventRepository.save(ActivityEventFactory.event(group.getId(), command.createdByUserId(),
				"EXPENSE_POSTED", "EXPENSE", savedExpense.getId(), "Expense posted", "{}", now));
		balanceProjectionUpdater.refresh(group.getId(), currencyCode);
		return savedExpense;
	}

	@Transactional
	public Expense cancelExpense(Long expenseId, Long cancelledByUserId, String reason) {
		var now = clock.instant();
		var expense = expenseRepository.findById(expenseId)
				.orElseThrow(() -> new ExpenseUpdateException("expense not found"));
		if (expense.getStatus() == ExpenseStatus.CANCELLED) {
			throw new ExpenseUpdateException("expense is already cancelled");
		}
		long expenseCreatedByUserId = expense.getCreatedByUserId();
		if(!Objects.equals(cancelledByUserId, expenseCreatedByUserId))
			throw  new ExpenseUpdateException(String.format("Expense created by: %d and actorUserId: %d don't match", expenseCreatedByUserId, cancelledByUserId));

		requireActiveMember(expense.getGroupId(), cancelledByUserId, "cancelling user");

		expense.setStatus(ExpenseStatus.CANCELLED);
		expense.setCancelledAt(now);
		expense.setCancelledByUserId(cancelledByUserId);
		expense.setCancellationReason(reason);
		expense.setUpdatedAt(now);
		var savedExpense = expenseRepository.save(expense);

		var originalEntries = ledgerEntryRepository.findBySourceTypeAndSourceIdOrderByIdAsc(LedgerSourceType.EXPENSE,
				expenseId);
		if (!originalEntries.isEmpty()) {
			ledgerEntryRepository.saveAll(ledgerService.reversalEntries(originalEntries, expenseId, now));
		}
		activityEventRepository.save(ActivityEventFactory.event(expense.getGroupId(), cancelledByUserId,
				"EXPENSE_CANCELLED", "EXPENSE", expense.getId(), "Expense cancelled", "{\"reason\":\""
						+ escapeJson(reason) + "\"}", now));
		balanceProjectionUpdater.refresh(expense.getGroupId(), expense.getCurrencyCode());
		return savedExpense;
	}

	private ExpenseSplit toExpenseSplit(Long expenseId, CalculatedSplit split, java.time.Instant now) {
		var row = new ExpenseSplit();
		row.setExpenseId(expenseId);
		row.setOwedByUserId(split.owedByUserId());
		row.setSplitType(split.splitType());
		row.setInputValue(split.inputValue());
		row.setAmountMinor(split.amountMinor());
		row.setCurrencyCode(split.currencyCode());
		row.setCreatedAt(now);
		return row;
	}

	private void requireActiveMember(Long groupId, Long userId, String label) {
		if (!groupMemberRepository.existsByGroupIdAndUserIdAndStatus(groupId, userId, GroupMemberStatus.ACTIVE)) {
			throw new IllegalArgumentException(label + " must be an active group member");
		}
	}

	private String escapeJson(String value) {
		if (value == null) {
			return "";
		}
		return value.replace("\\", "\\\\").replace("\"", "\\\"");
	}
}
