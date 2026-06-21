package com.kelvin.settlesense.domain.model.dto;

import com.kelvin.settlesense.domain.model.Expense;

import java.time.LocalDate;

public record ExpenseResponse(
		Long id,
		Long groupId,
		Long paidByUserId,
		String description,
		String currencyCode,
		long totalMinor,
		LocalDate expenseDate,
		String status) {

	public static ExpenseResponse from(Expense expense) {
		return new ExpenseResponse(expense.getId(), expense.getGroupId(), expense.getPaidByUserId(),
				expense.getDescription(), expense.getCurrencyCode(), expense.getTotalMinor(), expense.getExpenseDate(),
				expense.getStatus().name());
	}
}
