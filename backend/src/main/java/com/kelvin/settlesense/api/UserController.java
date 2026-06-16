package com.kelvin.settlesense.api;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.kelvin.settlesense.domain.model.User;
import com.kelvin.settlesense.domain.repository.UserRepository;
import com.kelvin.settlesense.domain.service.RegisterUserCommand;
import com.kelvin.settlesense.domain.service.UserWorkflowService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

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
	List<UserResponse> users() {
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

	record RegisterUserRequest(@NotBlank String displayName, @NotBlank @Email String email) {
		RegisterUserCommand toCommand() {
			return new RegisterUserCommand(displayName, email);
		}
	}

	record UserResponse(Long id, String displayName, String email, String status) {
		static UserResponse from(User user) {
			return new UserResponse(user.getId(), user.getDisplayName(), user.getEmail(), user.getStatus().name());
		}
	}
}
