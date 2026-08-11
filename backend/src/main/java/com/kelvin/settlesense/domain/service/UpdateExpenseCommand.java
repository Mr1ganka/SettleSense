package com.kelvin.settlesense.domain.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import com.kelvin.settlesense.domain.model.SplitType;

public record UpdateExpenseCommand(
		Long expenseId,
		Long paidByUserId,
		Map<Long, Long> payerInputsByUserId,
		String description,
		long totalMinor,
		LocalDate expenseDate,
		Long actorUserId,
		SplitType splitType,
		Map<Long, BigDecimal> splitInputsByUserId) {

	public UpdateExpenseCommand(
			Long expenseId,
			Long paidByUserId,
			String description,
			long totalMinor,
			LocalDate expenseDate,
			Long actorUserId,
			SplitType splitType,
			Map<Long, BigDecimal> splitInputsByUserId) {
		this(expenseId, paidByUserId, null, description, totalMinor, expenseDate, actorUserId, splitType, splitInputsByUserId);
	}
}
