package com.kelvin.settlesense.domain.service;

import java.time.Clock;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kelvin.settlesense.domain.model.User;
import com.kelvin.settlesense.domain.model.UserStatus;
import com.kelvin.settlesense.domain.repository.UserRepository;

@Service
public class UserWorkflowService {

	private final UserRepository userRepository;
	private final Clock clock;

	public UserWorkflowService(UserRepository userRepository, Clock clock) {
		this.userRepository = userRepository;
		this.clock = clock;
	}

	@Transactional
	public User registerUser(RegisterUserCommand command) {
		var displayName = requireText(command.displayName(), "displayName");
		var email = requireText(command.email(), "email").toLowerCase();
		if (userRepository.existsByEmailIgnoreCase(email)) {
			throw new IllegalArgumentException("user email already exists");
		}

		var now = clock.instant();
		var user = new User();
		user.setDisplayName(displayName);
		user.setEmail(email);
		user.setStatus(UserStatus.ACTIVE);
		user.setCreatedAt(now);
		user.setUpdatedAt(now);
		return userRepository.save(user);
	}

	private String requireText(String value, String fieldName) {
		if (value == null || value.trim().isEmpty()) {
			throw new IllegalArgumentException(fieldName + " is required");
		}
		return value.trim();
	}
}
