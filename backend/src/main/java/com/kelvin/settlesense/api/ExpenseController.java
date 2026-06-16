package com.kelvin.settlesense.api;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.kelvin.settlesense.domain.model.Expense;
import com.kelvin.settlesense.domain.model.SplitType;
import com.kelvin.settlesense.domain.service.ExpenseWorkflowService;
import com.kelvin.settlesense.domain.service.PostExpenseCommand;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@RestController
@RequestMapping("/api")
class ExpenseController {

	private final ExpenseWorkflowService expenseWorkflowService;

	ExpenseController(ExpenseWorkflowService expenseWorkflowService) {
		this.expenseWorkflowService = expenseWorkflowService;
	}

	@PostMapping("/groups/{groupId}/expenses")
	@ResponseStatus(HttpStatus.CREATED)
	ExpenseResponse postExpense(@PathVariable Long groupId, @Valid @RequestBody PostExpenseRequest request) {
		var expense = expenseWorkflowService.postExpense(request.toCommand(groupId));
		return ExpenseResponse.from(expense);
	}

	@PostMapping("/expenses/{expenseId}/cancel")
	ExpenseResponse cancelExpense(@PathVariable Long expenseId, @Valid @RequestBody CancelMoneyActionRequest request) {
		var expense = expenseWorkflowService.cancelExpense(expenseId, request.actorUserId(), request.reason());
		return ExpenseResponse.from(expense);
	}

	record PostExpenseRequest(
			@NotNull Long paidByUserId,
			@NotBlank String description,
			@Positive long totalMinor,
			@NotNull LocalDate expenseDate,
			@NotNull Long createdByUserId,
			@NotNull SplitType splitType,
			@NotEmpty Map<Long, BigDecimal> splitInputsByUserId) {

		PostExpenseCommand toCommand(Long groupId) {
			return new PostExpenseCommand(groupId, paidByUserId, description, totalMinor, expenseDate, createdByUserId,
					splitType, splitInputsByUserId);
		}
	}

	record CancelMoneyActionRequest(@NotNull Long actorUserId, String reason) {
	}

	record ExpenseResponse(
			Long id,
			Long groupId,
			Long paidByUserId,
			String description,
			String currencyCode,
			long totalMinor,
			LocalDate expenseDate,
			String status) {

		static ExpenseResponse from(Expense expense) {
			return new ExpenseResponse(expense.getId(), expense.getGroupId(), expense.getPaidByUserId(),
					expense.getDescription(), expense.getCurrencyCode(), expense.getTotalMinor(), expense.getExpenseDate(),
					expense.getStatus().name());
		}
	}
}
