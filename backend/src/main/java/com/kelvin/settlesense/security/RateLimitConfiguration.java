package com.kelvin.settlesense.security;

import java.util.List;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.data.redis.core.script.DefaultRedisScript;

@Configuration
@EnableConfigurationProperties(RateLimitProperties.class)
public class RateLimitConfiguration {

	@Bean
	RedisScript<List> rateLimitScript() {
		DefaultRedisScript<List> script = new DefaultRedisScript<>();
		script.setScriptText("""
				local key = KEYS[1]
				local capacity = tonumber(ARGV[1])
				local refillRate = tonumber(ARGV[2])
				local nowMillis = tonumber(ARGV[3])
				local cost = tonumber(ARGV[4])
				local ttlMillis = tonumber(ARGV[5])

				local tokens = tonumber(redis.call("HGET", key, "tokens"))
				local lastRefill = tonumber(redis.call("HGET", key, "lastRefill"))

				if tokens == nil then
					tokens = capacity
					lastRefill = nowMillis
				end

				local elapsedMillis = math.max(0, nowMillis - lastRefill)
				local refillTokens = (elapsedMillis / 1000.0) * refillRate
				tokens = math.min(capacity, tokens + refillTokens)
				lastRefill = nowMillis

				local allowed = 0
				local retryAfterSeconds = 0

				if tokens >= cost then
					tokens = tokens - cost
					allowed = 1
				else
					local deficit = cost - tokens
					retryAfterSeconds = math.max(1, math.ceil(deficit / refillRate))
				end

				redis.call("HSET", key, "tokens", tokens, "lastRefill", lastRefill)
				redis.call("PEXPIRE", key, ttlMillis)

				return { allowed, retryAfterSeconds }
				""");
		script.setResultType(List.class);
		return script;
	}
}
