package com.kelvin.settlesense.api;

import java.util.List;
import java.util.Objects;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.kelvin.settlesense.domain.model.User;
import com.kelvin.settlesense.domain.model.dto.RegisterUserRequest;
import com.kelvin.settlesense.domain.model.dto.UpdateUserRequest;
import com.kelvin.settlesense.domain.model.dto.UserResponse;
import com.kelvin.settlesense.domain.repository.UserRepository;
import com.kelvin.settlesense.domain.service.UserWorkflowService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/users")
class UserController {

	private final UserWorkflowService userWorkflowService;
	private final UserRepository userRepository;

	UserController(UserWorkflowService userWorkflowService, UserRepository userRepository) {
		this.userWorkflowService = userWorkflowService;
		this.userRepository = userRepository;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	UserResponse registerUser(@Valid @RequestBody RegisterUserRequest request) {
		return UserResponse.from(userWorkflowService.registerUser(request.toCommand()));
	}

	@GetMapping
	List<UserResponse> users(@org.springframework.web.bind.annotation.RequestParam(required = false) String search) {
		if (search != null && !search.trim().isEmpty()) {
			return userRepository.searchUsers(search.trim()).stream()
					.map(UserResponse::from)
					.toList();
		}
		return userRepository.findAllByOrderByIdAsc().stream()
				.map(UserResponse::from)
				.toList();
	}


	@GetMapping("/{userId}")
	UserResponse user(@PathVariable Long userId) {
		return userRepository.findById(userId)
				.map(UserResponse::from)
				.orElseThrow(() -> new IllegalArgumentException("user not found"));
	}

	@PutMapping("/{userId}")
	UserResponse updateUser(@PathVariable Long userId, @Valid @RequestBody UpdateUserRequest request) {
		var actorUserId = currentUserId(userId);
		if (!Objects.equals(actorUserId, userId)) {
			throw new IllegalArgumentException("cannot update another user");
		}
		return UserResponse.from(userWorkflowService.updateUser(userId, request.displayName()));
	}

	private Long currentUserId(Long fallbackUserId) {
		var authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication != null && authentication.getPrincipal() instanceof User user && user.getId() != null) {
			return user.getId();
		}
		return fallbackUserId;
	}
}
