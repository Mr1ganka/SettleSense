package com.kelvin.settlesense.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kelvin.settlesense.domain.model.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class RateLimitFilterTests {

	private RedisTokenBucketRateLimiter rateLimiter;
	private RateLimitFilter filter;

	@BeforeEach
	void setUp() {
		rateLimiter = org.mockito.Mockito.mock(RedisTokenBucketRateLimiter.class);
		filter = new RateLimitFilter(rateLimiter);
	}

	@AfterEach
	void tearDown() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void allowsLimitedUsersEndpointWhenRequestIsWithinBudget() throws Exception {
		var user = new User();
		user.setId(42L);
		SecurityContextHolder.getContext().setAuthentication(
				new UsernamePasswordAuthenticationToken(user, null));

		when(rateLimiter.check("users", "user:42")).thenReturn(new RateLimitDecision(true, 0));

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/users");
		MockHttpServletResponse response = new MockHttpServletResponse();
		MockFilterChain chain = new MockFilterChain();

		filter.doFilter(request, response, chain);

		assertThat(response.getStatus()).isEqualTo(200);
		verify(rateLimiter).check("users", "user:42");
	}

	@Test
	void rejectsLimitedGroupsEndpointWhenBudgetIsExceeded() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/groups");
		request.setRemoteAddr("203.0.113.10");
		MockHttpServletResponse response = new MockHttpServletResponse();
		MockFilterChain chain = new MockFilterChain();

		when(rateLimiter.check("groups", "ip:203.0.113.10")).thenReturn(new RateLimitDecision(false, 17));

		filter.doFilter(request, response, chain);

		assertThat(response.getStatus()).isEqualTo(429);
		assertThat(response.getHeader("Retry-After")).isEqualTo("17");
		assertThat(response.getContentAsString()).contains("rate limit exceeded");
	}

	@Test
	void doesNotRateLimitUnlistedEndpoints() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/system/status");
		MockHttpServletResponse response = new MockHttpServletResponse();
		MockFilterChain chain = new MockFilterChain();

		filter.doFilter(request, response, chain);

		assertThat(response.getStatus()).isEqualTo(200);
		verify(rateLimiter, never()).check(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
	}

	@Test
	void fallsBackToIpWhenNoAuthenticatedPrincipalExists() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/users");
		request.setRemoteAddr("198.51.100.5");
		MockHttpServletResponse response = new MockHttpServletResponse();
		MockFilterChain chain = new MockFilterChain();

		when(rateLimiter.check("users", "ip:198.51.100.5")).thenReturn(new RateLimitDecision(true, 0));

		filter.doFilter(request, response, chain);

		assertThat(response.getStatus()).isEqualTo(200);
		verify(rateLimiter).check("users", "ip:198.51.100.5");
	}
}
