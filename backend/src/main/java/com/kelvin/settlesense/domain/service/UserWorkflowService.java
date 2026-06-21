package com.kelvin.settlesense.domain.service;

import java.time.Clock;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kelvin.settlesense.domain.model.User;
import com.kelvin.settlesense.domain.model.UserStatus;
import com.kelvin.settlesense.domain.model.dto.RegisterUserDto;
import com.kelvin.settlesense.domain.repository.UserRepository;

@Service
public class UserWorkflowService {

	private final UserRepository userRepository;
	private final Clock clock;
	private final PasswordEncoder passwordEncoder;

	public UserWorkflowService(UserRepository userRepository, Clock clock, PasswordEncoder passwordEncoder) {
		this.userRepository = userRepository;
		this.clock = clock;
        this.passwordEncoder = passwordEncoder;
	}

	@Transactional
	public User registerUser(RegisterUserDto command) {
		var email = requireText(command.email(), "email").toLowerCase();

		if (userRepository.existsByEmailIgnoreCase(email)) {
			throw new IllegalArgumentException("user email already exists");
		}

		var displayName = requireText(command.displayName(), "displayName");
		var password = requireText(command.password(), "password");
		String hashedPass = hashPassword(password);


		var now = clock.instant();
		var user = new User();
		user.setDisplayName(displayName);
		user.setEmail(email);
		user.setStatus(UserStatus.ACTIVE);
		user.setCreatedAt(now);
		user.setUpdatedAt(now);
		user.setPasswordHash(hashedPass);
		return userRepository.save(user);
	}

	@Transactional
	public User updateUser(Long userId, String displayName) {
		var user = userRepository.findById(userId)
				.orElseThrow(() -> new IllegalArgumentException("user not found"));

		user.setDisplayName(requireText(displayName, "displayName"));
		user.setUpdatedAt(clock.instant());
		return userRepository.save(user);
	}

	public String hashPassword(String password) {
		return passwordEncoder.encode(password);
	}

	private String requireText(String value, String fieldName) {
		if (value == null || value.trim().isEmpty()) {
			throw new IllegalArgumentException(fieldName + " is required");
		}
		return value.trim();
	}
}
