package com.kelvin.settlesense.security;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Validated
@ConfigurationProperties(prefix = "rate-limit")
public record RateLimitProperties(
		@Min(1) int capacity,
		@Positive double refillTokensPerSecond,
		@NotNull Duration bucketTtl,
		@NotBlank String redisKeyPrefix) {
}
