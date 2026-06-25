package com.kelvin.settlesense.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

class RedisTokenBucketRateLimiterTests {

	private StringRedisTemplate redisTemplate;
	private RedisScript<List> rateLimitScript;
	private RateLimitProperties properties;
	private Clock clock;
	private RedisTokenBucketRateLimiter rateLimiter;

	@BeforeEach
	void setUp() {
		redisTemplate = org.mockito.Mockito.mock(StringRedisTemplate.class);
		rateLimitScript = new RateLimitConfiguration().rateLimitScript();
		properties = new RateLimitProperties(60, 1.0, Duration.ofMinutes(15), "settlesense:ratelimit");
		clock = Clock.fixed(Instant.parse("2026-06-22T10:15:30Z"), ZoneOffset.UTC);
		rateLimiter = new RedisTokenBucketRateLimiter(redisTemplate, rateLimitScript, properties, clock);
	}

	@Test
	void checkUsesRedisTokenBucketResult() {
		when(redisTemplate.execute(any(), any(List.class), any(Object[].class))).thenReturn(List.of(1L, 0L));

		RateLimitDecision decision = rateLimiter.check("users", "user:42");

		assertThat(decision.allowed()).isTrue();
		assertThat(decision.retryAfterSeconds()).isZero();

		ArgumentCaptor<List<String>> keysCaptor = ArgumentCaptor.forClass(List.class);
		ArgumentCaptor<Object[]> argsCaptor = ArgumentCaptor.forClass(Object[].class);
		verify(redisTemplate).execute(eq(rateLimitScript), keysCaptor.capture(), argsCaptor.capture());

		assertThat(keysCaptor.getValue()).containsExactly("settlesense:ratelimit:users:user:42");
		assertThat(argsCaptor.getValue()).containsExactly(
				"60",
				"1.0",
				String.valueOf(clock.millis()),
				"1",
				String.valueOf(properties.bucketTtl().toMillis()));
	}

	@Test
	void checkReturnsRetryAfterWhenBucketIsEmpty() {
		when(redisTemplate.execute(any(), any(List.class), any(Object[].class))).thenReturn(List.of(0L, 7L));

		RateLimitDecision decision = rateLimiter.check("groups", "ip:127.0.0.1");

		assertThat(decision.allowed()).isFalse();
		assertThat(decision.retryAfterSeconds()).isEqualTo(7L);
	}

	@Test
	void checkFailsOpenWhenRedisIsUnavailable() {
		when(redisTemplate.execute(any(), any(List.class), any(Object[].class))).thenThrow(new RuntimeException("redis down"));

		RateLimitDecision decision = rateLimiter.check("users", "user:42");

		assertThat(decision.allowed()).isTrue();
		assertThat(decision.retryAfterSeconds()).isZero();
	}
}
