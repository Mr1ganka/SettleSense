package com.kelvin.settlesense.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.kelvin.settlesense.domain.model.SplitType;

class SplitCalculatorTests {

	private final SplitCalculator calculator = new SplitCalculator();

	@Test
	void equalSplitDistributesRemainderDeterministicallyByUserId() {
		var splits = calculator.calculate(100000, "inr", SplitType.EQUAL,
				Map.of(30L, BigDecimal.ONE, 10L, BigDecimal.ONE, 20L, BigDecimal.ONE));

		assertThat(splits).extracting(CalculatedSplit::owedByUserId).containsExactly(10L, 20L, 30L);
		assertThat(splits).extracting(CalculatedSplit::amountMinor).containsExactly(33334L, 33333L, 33333L);
		assertThat(splits).extracting(CalculatedSplit::currencyCode).containsOnly("INR");
	}

	@Test
	void exactSplitRequiresAmountsToMatchTotal() {
		assertThatThrownBy(() -> calculator.calculate(1000, "INR", SplitType.EXACT,
				Map.of(1L, new BigDecimal("600"), 2L, new BigDecimal("399"))))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("must sum to totalMinor");
	}

	@Test
	void percentageSplitRequiresOneHundredPercentAndStoresFinalMinorAmounts() {
		var splits = calculator.calculate(1000, "INR", SplitType.PERCENTAGE,
				Map.of(1L, new BigDecimal("33.33"), 2L, new BigDecimal("33.33"), 3L, new BigDecimal("33.34")));

		assertThat(splits).extracting(CalculatedSplit::amountMinor).containsExactly(334L, 333L, 333L);
		assertThat(splits).extracting(CalculatedSplit::inputValue)
				.containsExactly(new BigDecimal("33.33"), new BigDecimal("33.33"), new BigDecimal("33.34"));
	}

	@Test
	void shareSplitProducesAmountsThatSumToTotal() {
		var splits = calculator.calculate(1001, "INR", SplitType.SHARE,
				Map.of(1L, new BigDecimal("1"), 2L, new BigDecimal("2")));

		assertThat(splits).extracting(CalculatedSplit::amountMinor).containsExactly(334L, 667L);
		assertThat(splits).satisfies(splitRows -> assertThat(splitRows.stream()
				.mapToLong(CalculatedSplit::amountMinor)
				.sum()).isEqualTo(1001));
	}
}
