package com.kelvin.settlesense.api;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.kelvin.settlesense.domain.model.Expense;
import com.kelvin.settlesense.domain.model.SplitType;
import com.kelvin.settlesense.domain.model.dto.CancelMoneyActionRequest;
import com.kelvin.settlesense.domain.model.dto.ExpenseResponse;
import com.kelvin.settlesense.domain.model.dto.PostExpenseRequest;
import com.kelvin.settlesense.domain.repository.ExpenseRepository;
import com.kelvin.settlesense.domain.service.ExpenseWorkflowService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api")
class ExpenseController {

	private final ExpenseWorkflowService expenseWorkflowService;
	private final ExpenseRepository expenseRepository;

	ExpenseController(ExpenseWorkflowService expenseWorkflowService, ExpenseRepository expenseRepository) {
		this.expenseWorkflowService = expenseWorkflowService;
		this.expenseRepository = expenseRepository;
	}

	@GetMapping("/groups/{groupId}/expenses")
	List<ExpenseResponse> listExpenses(@PathVariable Long groupId) {
		return expenseRepository.findByGroupIdOrderByIdDesc(groupId).stream()
				.map(ExpenseResponse::from)
				.toList();
	}

	@PostMapping("/groups/{groupId}/expenses")
	@ResponseStatus(HttpStatus.CREATED)
	ExpenseResponse postExpense(@PathVariable Long groupId, @Valid @RequestBody PostExpenseRequest request) {
		var expense = expenseWorkflowService.postExpense(request.toCommand(groupId, currentUserId(request.createdByUserId())));
		return ExpenseResponse.from(expense);
	}

	@PostMapping("/expenses/{expenseId}/cancel")
	ExpenseResponse cancelExpense(@PathVariable Long expenseId, @Valid @RequestBody CancelMoneyActionRequest request) {
		var expense = expenseWorkflowService.cancelExpense(expenseId, currentUserId(request.actorUserId()),
				request.reason());
		return ExpenseResponse.from(expense);
	}

	@org.springframework.web.bind.annotation.PutMapping("/expenses/{expenseId}")
	ExpenseResponse editExpense(@PathVariable Long expenseId, @Valid @RequestBody com.kelvin.settlesense.domain.model.dto.UpdateExpenseRequest request) {
		var expense = expenseWorkflowService.editExpense(request.toCommand(expenseId, currentUserId(request.actorUserId())));
		return ExpenseResponse.from(expense);
	}

	@PostMapping("/expenses/{expenseId}/receipt")
	ExpenseResponse uploadReceipt(@PathVariable Long expenseId,
			@org.springframework.web.bind.annotation.RequestParam("file") org.springframework.web.multipart.MultipartFile file) throws java.io.IOException {
		Long actorId = currentUserId(null);
		var expense = expenseWorkflowService.attachReceipt(expenseId, actorId, file.getOriginalFilename(), file.getBytes());
		return ExpenseResponse.from(expense);
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
