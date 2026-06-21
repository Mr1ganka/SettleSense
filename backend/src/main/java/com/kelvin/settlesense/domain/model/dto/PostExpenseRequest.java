package com.kelvin.settlesense.domain.model.dto;

import com.kelvin.settlesense.domain.model.SplitType;
import com.kelvin.settlesense.domain.service.PostExpenseCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

public record PostExpenseRequest(
		@NotNull Long paidByUserId,
		@NotBlank String description,
		@Positive long totalMinor,
		@NotNull LocalDate expenseDate,
		@NotNull Long createdByUserId,
		@NotNull SplitType splitType,
		@NotEmpty Map<Long, BigDecimal> splitInputsByUserId) {

	public PostExpenseCommand toCommand(Long groupId, Long actorUserId) {
		return new PostExpenseCommand(groupId, paidByUserId, description, totalMinor, expenseDate, actorUserId,
				splitType, splitInputsByUserId);
	}
}
