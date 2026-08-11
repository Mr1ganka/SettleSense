package com.kelvin.settlesense.domain.model.dto;

import com.kelvin.settlesense.domain.model.SplitType;
import com.kelvin.settlesense.domain.service.UpdateExpenseCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

public record UpdateExpenseRequest(
		@NotNull Long paidByUserId,
		Map<Long, Long> payerInputsByUserId,
		@NotBlank String description,
		@Positive long totalMinor,
		@NotNull LocalDate expenseDate,
		@NotNull Long actorUserId,
		@NotNull SplitType splitType,
		@NotEmpty Map<Long, BigDecimal> splitInputsByUserId) {

	public UpdateExpenseRequest(
			Long paidByUserId,
			String description,
			long totalMinor,
			LocalDate expenseDate,
			Long actorUserId,
			SplitType splitType,
			Map<Long, BigDecimal> splitInputsByUserId) {
		this(paidByUserId, null, description, totalMinor, expenseDate, actorUserId, splitType, splitInputsByUserId);
	}

	public UpdateExpenseCommand toCommand(Long expenseId, Long actorUserIdOverride) {
		return new UpdateExpenseCommand(expenseId, paidByUserId, payerInputsByUserId, description, totalMinor, expenseDate,
				actorUserIdOverride != null ? actorUserIdOverride : actorUserId, splitType, splitInputsByUserId);
	}
}
