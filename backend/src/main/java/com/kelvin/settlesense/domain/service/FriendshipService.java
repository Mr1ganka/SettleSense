package com.kelvin.settlesense.domain.service;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kelvin.settlesense.domain.model.Friendship;
import com.kelvin.settlesense.domain.model.FriendshipStatus;
import com.kelvin.settlesense.domain.model.User;
import com.kelvin.settlesense.domain.model.dto.FriendshipResponse;
import com.kelvin.settlesense.domain.model.dto.UserResponse;
import com.kelvin.settlesense.domain.repository.FriendshipRepository;
import com.kelvin.settlesense.domain.repository.UserRepository;

@Service
public class FriendshipService {

	private final FriendshipRepository friendshipRepository;
	private final UserRepository userRepository;
	private final Clock clock;

	public FriendshipService(FriendshipRepository friendshipRepository, UserRepository userRepository, Clock clock) {
		this.friendshipRepository = friendshipRepository;
		this.userRepository = userRepository;
		this.clock = clock;
	}

	@Transactional
	public Friendship requestFriendship(Long requesterUserId, Long addresseeUserId) {
		if (requesterUserId.equals(addresseeUserId)) {
			throw new IllegalArgumentException("friendship users must be different");
		}
		if (!userRepository.existsById(requesterUserId) || !userRepository.existsById(addresseeUserId)) {
			throw new IllegalArgumentException("both friendship users must exist");
		}
		
		Optional<Friendship> existing = friendshipRepository.findFriendshipBetween(requesterUserId, addresseeUserId);
		if (existing.isPresent()) {
			Friendship f = existing.get();
			if (f.getStatus() == FriendshipStatus.ACCEPTED || f.getStatus() == FriendshipStatus.PENDING) {
				throw new IllegalArgumentException("friendship already exists for this user pair");
			} else {
				// Re-open cancelled or rejected request
				f.setRequesterUserId(requesterUserId);
				f.setAddresseeUserId(addresseeUserId);
				f.setStatus(FriendshipStatus.PENDING);
				f.setUpdatedAt(clock.instant());
				return friendshipRepository.save(f);
			}
		}


		var now = clock.instant();
		var friendship = new Friendship();
		friendship.setRequesterUserId(requesterUserId);
		friendship.setAddresseeUserId(addresseeUserId);
		friendship.setStatus(FriendshipStatus.PENDING);
		friendship.setCreatedAt(now);
		friendship.setUpdatedAt(now);
		return friendshipRepository.save(friendship);
	}

	@Transactional
	public Friendship requestFriendshipByEmail(Long requesterUserId, String email) {
		if (email == null || email.trim().isEmpty()) {
			throw new IllegalArgumentException("Email is required");
		}
		User targetUser = userRepository.findByEmailIgnoreCase(email.trim())
				.orElseThrow(() -> new IllegalArgumentException("User with email '" + email + "' not found"));
		return requestFriendship(requesterUserId, targetUser.getId());
	}

	@Transactional
	public Friendship acceptFriendship(Long friendshipId, Long actorUserId) {
		Friendship friendship = friendshipRepository.findById(friendshipId)
				.orElseThrow(() -> new IllegalArgumentException("Friendship request not found"));
		if (!Objects.equals(friendship.getAddresseeUserId(), actorUserId)) {
			throw new IllegalArgumentException("Only the recipient can accept this friendship request");
		}
		if (friendship.getStatus() != FriendshipStatus.PENDING) {
			throw new IllegalArgumentException("Friendship request is not in PENDING status");
		}
		friendship.setStatus(FriendshipStatus.ACCEPTED);
		friendship.setUpdatedAt(clock.instant());
		return friendshipRepository.save(friendship);
	}

	@Transactional
	public Friendship rejectFriendship(Long friendshipId, Long actorUserId) {
		Friendship friendship = friendshipRepository.findById(friendshipId)
				.orElseThrow(() -> new IllegalArgumentException("Friendship request not found"));
		if (!Objects.equals(friendship.getAddresseeUserId(), actorUserId) && !Objects.equals(friendship.getRequesterUserId(), actorUserId)) {
			throw new IllegalArgumentException("Not authorized to modify this friendship request");
		}
		friendship.setStatus(FriendshipStatus.CANCELLED);
		friendship.setUpdatedAt(clock.instant());
		return friendshipRepository.save(friendship);
	}

	@Transactional(readOnly = true)
	public List<UserResponse> getAcceptedFriends(Long userId) {
		List<Friendship> friendships = friendshipRepository.findByUserIdAndStatus(userId, FriendshipStatus.ACCEPTED);
		List<UserResponse> friends = new ArrayList<>();
		for (Friendship f : friendships) {
			Long friendId = f.getRequesterUserId().equals(userId) ? f.getAddresseeUserId() : f.getRequesterUserId();
			userRepository.findById(friendId).ifPresent(user -> friends.add(UserResponse.from(user)));
		}
		return friends;
	}

	@Transactional(readOnly = true)
	public List<FriendshipResponse> getPendingRequests(Long userId) {
		List<Friendship> pendingList = friendshipRepository.findByUserIdAndStatus(userId, FriendshipStatus.PENDING);
		List<FriendshipResponse> responses = new ArrayList<>();
		for (Friendship f : pendingList) {
			User requester = userRepository.findById(f.getRequesterUserId()).orElse(null);
			User addressee = userRepository.findById(f.getAddresseeUserId()).orElse(null);
			if (requester != null && addressee != null) {
				responses.add(new FriendshipResponse(
						f.getId(),
						UserResponse.from(requester),
						UserResponse.from(addressee),
						f.getStatus().name(),
						f.getCreatedAt(),
						f.getUpdatedAt()
				));
			}
		}
		return responses;
	}

	@Transactional(readOnly = true)
	public boolean areFriends(Long user1Id, Long user2Id) {
		if (user1Id.equals(user2Id)) return true;
		return friendshipRepository.findFriendshipBetween(user1Id, user2Id)
				.map(f -> f.getStatus() == FriendshipStatus.ACCEPTED)
				.orElse(false);
	}
}
