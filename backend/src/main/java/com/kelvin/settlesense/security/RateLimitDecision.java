package com.kelvin.settlesense.security;

record RateLimitDecision(boolean allowed, long retryAfterSeconds) {

	static RateLimitDecision granted() {
		return new RateLimitDecision(true, 0L);
	}
}
