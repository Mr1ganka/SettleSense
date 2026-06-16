package com.kelvin.settlesense.domain.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.kelvin.settlesense.domain.model.SplitType;
import org.springframework.stereotype.Service;

@Service
public class SplitCalculator {

	public List<CalculatedSplit> calculate(long totalMinor, String currencyCode, SplitType splitType,
			Map<Long, BigDecimal> inputValuesByUserId) {
		MoneyRules.requirePositive(totalMinor, "totalMinor");
		var normalizedCurrency = MoneyRules.normalizeCurrencyCode(currencyCode);
		if (inputValuesByUserId == null || inputValuesByUserId.isEmpty()) {
			throw new IllegalArgumentException("at least one split participant is required");
		}

		var orderedInputs = orderedInputs(inputValuesByUserId);
		return switch (splitType) {
			case EQUAL -> calculateEqual(totalMinor, normalizedCurrency, orderedInputs);
			case EXACT -> calculateExact(totalMinor, normalizedCurrency, orderedInputs);
			case PERCENTAGE -> calculatePercentage(totalMinor, normalizedCurrency, orderedInputs);
			case SHARE -> calculateShare(totalMinor, normalizedCurrency, orderedInputs);
		};
	}

	private List<CalculatedSplit> calculateEqual(long totalMinor, String currencyCode,
			LinkedHashMap<Long, BigDecimal> inputs) {
		var count = inputs.size();
		var base = totalMinor / count;
		var remainder = totalMinor % count;
		var splits = new ArrayList<CalculatedSplit>();
		var index = 0;
		for (var entry : inputs.entrySet()) {
			var amount = base + (index < remainder ? 1 : 0);
			splits.add(new CalculatedSplit(entry.getKey(), SplitType.EQUAL, BigDecimal.ONE, amount, currencyCode));
			index++;
		}
		return splits;
	}

	private List<CalculatedSplit> calculateExact(long totalMinor, String currencyCode,
			LinkedHashMap<Long, BigDecimal> inputs) {
		var splits = new ArrayList<CalculatedSplit>();
		long sum = 0;
		for (var entry : inputs.entrySet()) {
			var amount = toWholeMinorUnits(entry.getValue(), "exact split input");
			if (amount < 0) {
				throw new IllegalArgumentException("exact split amount cannot be negative");
			}
			sum += amount;
			splits.add(new CalculatedSplit(entry.getKey(), SplitType.EXACT, entry.getValue(), amount, currencyCode));
		}
		requireSum(totalMinor, sum, "exact split amounts");
		return splits;
	}

	private List<CalculatedSplit> calculatePercentage(long totalMinor, String currencyCode,
			LinkedHashMap<Long, BigDecimal> inputs) {
		var totalPercent = inputs.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
		if (totalPercent.compareTo(new BigDecimal("100")) != 0) {
			throw new IllegalArgumentException("percentage split inputs must sum to 100");
		}

		var rawAmounts = inputs.entrySet().stream()
				.map(entry -> new RawSplit(entry.getKey(), entry.getValue(),
						entry.getValue().multiply(BigDecimal.valueOf(totalMinor)).divide(new BigDecimal("100"), 6,
								RoundingMode.DOWN)))
				.toList();
		return allocateRemainder(totalMinor, currencyCode, SplitType.PERCENTAGE, rawAmounts);
	}

	private List<CalculatedSplit> calculateShare(long totalMinor, String currencyCode,
			LinkedHashMap<Long, BigDecimal> inputs) {
		var totalShares = inputs.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
		if (totalShares.compareTo(BigDecimal.ZERO) <= 0) {
			throw new IllegalArgumentException("share split inputs must sum to more than zero");
		}
		if (inputs.values().stream().anyMatch(value -> value.compareTo(BigDecimal.ZERO) < 0)) {
			throw new IllegalArgumentException("share split inputs cannot be negative");
		}

		var rawAmounts = inputs.entrySet().stream()
				.map(entry -> new RawSplit(entry.getKey(), entry.getValue(),
						entry.getValue().multiply(BigDecimal.valueOf(totalMinor)).divide(totalShares, 6,
								RoundingMode.DOWN)))
				.toList();
		return allocateRemainder(totalMinor, currencyCode, SplitType.SHARE, rawAmounts);
	}

	private List<CalculatedSplit> allocateRemainder(long totalMinor, String currencyCode, SplitType splitType,
			List<RawSplit> rawAmounts) {
		var splits = new ArrayList<CalculatedSplit>();
		long allocated = 0;
		for (var raw : rawAmounts) {
			var amount = raw.rawAmountMinor().setScale(0, RoundingMode.DOWN).longValueExact();
			allocated += amount;
			splits.add(new CalculatedSplit(raw.userId(), splitType, raw.inputValue(), amount, currencyCode));
		}

		var remainder = totalMinor - allocated;
		for (int index = 0; index < remainder; index++) {
			var existing = splits.get(index);
			splits.set(index, new CalculatedSplit(existing.owedByUserId(), existing.splitType(), existing.inputValue(),
					existing.amountMinor() + 1, existing.currencyCode()));
		}
		return splits;
	}

	private LinkedHashMap<Long, BigDecimal> orderedInputs(Map<Long, BigDecimal> inputValuesByUserId) {
		if (inputValuesByUserId.keySet().stream().anyMatch(userId -> userId == null)) {
			throw new IllegalArgumentException("split user id is required");
		}
		if (inputValuesByUserId.values().stream().anyMatch(value -> value == null)) {
			throw new IllegalArgumentException("split input value is required");
		}
		var ordered = new LinkedHashMap<Long, BigDecimal>();
		inputValuesByUserId.entrySet().stream()
				.sorted(Map.Entry.comparingByKey())
				.forEach(entry -> ordered.put(entry.getKey(), entry.getValue()));
		return ordered;
	}

	private long toWholeMinorUnits(BigDecimal value, String fieldName) {
		try {
			return value.setScale(0, RoundingMode.UNNECESSARY).longValueExact();
		}
		catch (ArithmeticException ex) {
			throw new IllegalArgumentException(fieldName + " must be whole minor units", ex);
		}
	}

	private void requireSum(long expected, long actual, String label) {
		if (expected != actual) {
			throw new IllegalArgumentException(label + " must sum to totalMinor");
		}
	}

	private record RawSplit(Long userId, BigDecimal inputValue, BigDecimal rawAmountMinor) {
	}
}
