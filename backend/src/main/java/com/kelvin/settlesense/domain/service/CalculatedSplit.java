package com.kelvin.settlesense.domain.service;

import java.math.BigDecimal;

import com.kelvin.settlesense.domain.model.SplitType;

public record CalculatedSplit(
		Long owedByUserId,
		SplitType splitType,
		BigDecimal inputValue,
		long amountMinor,
		String currencyCode) {
}
