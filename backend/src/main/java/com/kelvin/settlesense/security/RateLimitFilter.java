package com.kelvin.settlesense.security;

import java.util.Set;

import com.kelvin.settlesense.domain.model.User;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

	private static final Set<String> LIMITED_PATHS = Set.of("/api/users", "/api/groups");

	private final RedisTokenBucketRateLimiter rateLimiter;

	public RateLimitFilter(RedisTokenBucketRateLimiter rateLimiter) {
		this.rateLimiter = rateLimiter;
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		if (!"GET".equalsIgnoreCase(request.getMethod())) {
			return true;
		}

		String path = normalizedPath(request);
		return !LIMITED_PATHS.contains(path);
	}

	@Override
	protected void doFilterInternal(
			HttpServletRequest request,
			HttpServletResponse response,
			FilterChain filterChain) throws ServletException, java.io.IOException {

		String path = normalizedPath(request);
		String endpoint = path.substring("/api/".length());
		String subject = resolveSubject(request);
		RateLimitDecision decision = rateLimiter.check(endpoint, subject);

		if (!decision.allowed()) {
			writeTooManyRequests(response, decision.retryAfterSeconds());
			return;
		}

		filterChain.doFilter(request, response);
	}

	private String resolveSubject(HttpServletRequest request) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication != null && authentication.getPrincipal() instanceof User user) {
			if (user.getId() != null) {
				return "user:" + user.getId();
			}
			if (user.getEmail() != null && !user.getEmail().isBlank()) {
				return "user:" + user.getEmail();
			}
		}

		return "ip:" + request.getRemoteAddr();
	}

	private String normalizedPath(HttpServletRequest request) {
		return request.getRequestURI().substring(request.getContextPath().length());
	}

	private void writeTooManyRequests(HttpServletResponse response, long retryAfterSeconds) throws java.io.IOException {
		response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");
		response.setHeader("Retry-After", String.valueOf(Math.max(1L, retryAfterSeconds)));
		response.getWriter().write("{\"message\":\"rate limit exceeded\"}");
	}
}
