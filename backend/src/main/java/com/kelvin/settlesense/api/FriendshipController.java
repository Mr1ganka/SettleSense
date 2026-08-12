package com.kelvin.settlesense.api;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.kelvin.settlesense.domain.model.User;
import com.kelvin.settlesense.domain.model.dto.FriendshipResponse;
import com.kelvin.settlesense.domain.model.dto.SendFriendRequest;
import com.kelvin.settlesense.domain.model.dto.UserResponse;
import com.kelvin.settlesense.domain.service.FriendshipService;

@RestController
@RequestMapping("/api/friends")
public class FriendshipController {

	private final FriendshipService friendshipService;

	public FriendshipController(FriendshipService friendshipService) {
		this.friendshipService = friendshipService;
	}

	@GetMapping
	public List<UserResponse> getFriends() {
		Long userId = currentUserIdRequired();
		return friendshipService.getAcceptedFriends(userId);
	}

	@GetMapping("/requests")
	public List<FriendshipResponse> getPendingRequests() {
		Long userId = currentUserIdRequired();
		return friendshipService.getPendingRequests(userId);
	}

	@PostMapping("/request")
	@ResponseStatus(HttpStatus.CREATED)
	public FriendshipResponse sendRequest(@RequestBody SendFriendRequest request) {
		Long currentUserId = currentUserIdRequired();
		var friendship = (request.email() != null && !request.email().trim().isEmpty())
				? friendshipService.requestFriendshipByEmail(currentUserId, request.email())
				: friendshipService.requestFriendship(currentUserId, request.targetUserId());
		
		return friendshipService.getPendingRequests(currentUserId).stream()
				.filter(r -> r.id().equals(friendship.getId()))
				.findFirst()
				.orElse(null);
	}

	@PostMapping("/{friendshipId}/accept")
	public FriendshipResponse acceptRequest(@PathVariable Long friendshipId) {
		Long currentUserId = currentUserIdRequired();
		friendshipService.acceptFriendship(friendshipId, currentUserId);
		return null;
	}

	@PostMapping("/{friendshipId}/reject")
	public void rejectRequest(@PathVariable Long friendshipId) {
		Long currentUserId = currentUserIdRequired();
		friendshipService.rejectFriendship(friendshipId, currentUserId);
	}

	private Long currentUserIdRequired() {
		var authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication != null && authentication.getPrincipal() instanceof User user && user.getId() != null) {
			return user.getId();
		}
		throw new IllegalStateException("Authentication required");
	}
}
