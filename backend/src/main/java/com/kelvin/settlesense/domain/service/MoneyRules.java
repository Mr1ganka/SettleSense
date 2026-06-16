package com.kelvin.settlesense.domain.service;

import java.util.Objects;

public final class MoneyRules {

	private MoneyRules() {
	}

	public static void requirePositive(long amountMinor, String fieldName) {
		if (amountMinor <= 0) {
			throw new IllegalArgumentException(fieldName + " must be greater than zero");
		}
	}

	public static String normalizeCurrencyCode(String currencyCode) {
		var normalized = Objects.requireNonNull(currencyCode, "currencyCode is required").trim().toUpperCase();
		if (!normalized.matches("[A-Z]{3}")) {
			throw new IllegalArgumentException("currencyCode must be an ISO-style 3 letter code");
		}
		return normalized;
	}

	public static void requireSameCurrency(String expectedCurrencyCode, String actualCurrencyCode) {
		var expected = normalizeCurrencyCode(expectedCurrencyCode);
		var actual = normalizeCurrencyCode(actualCurrencyCode);
		if (!expected.equals(actual)) {
			throw new IllegalArgumentException("currency mismatch: expected " + expected + " but got " + actual);
		}
	}
}
