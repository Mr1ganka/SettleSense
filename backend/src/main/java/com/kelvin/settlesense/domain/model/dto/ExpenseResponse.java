package com.kelvin.settlesense.domain.model.dto;

import com.kelvin.settlesense.domain.model.Expense;
import com.kelvin.settlesense.domain.model.ExpenseCategory;

import java.time.LocalDate;

public record ExpenseResponse(
		Long id,
		Long groupId,
		Long paidByUserId,
		String paidByDisplayName,
		String description,
		String currencyCode,
		long totalMinor,
		LocalDate expenseDate,
		String status,
		ExpenseCategory category,
		String receiptUrl) {

	public static ExpenseResponse from(Expense expense, String paidByDisplayName) {
		return new ExpenseResponse(expense.getId(), expense.getGroupId(), expense.getPaidByUserId(),
				paidByDisplayName, expense.getDescription(), expense.getCurrencyCode(), expense.getTotalMinor(),
				expense.getExpenseDate(), expense.getStatus().name(),
				expense.getCategory() != null ? expense.getCategory() : ExpenseCategory.GENERAL,
				expense.getReceiptUrl());
	}

	public static ExpenseResponse from(Expense expense) {
		return from(expense, null);
	}
}
