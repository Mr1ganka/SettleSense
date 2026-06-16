package com.kelvin.settlesense.domain.service;

import java.time.Clock;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kelvin.settlesense.domain.model.Friendship;
import com.kelvin.settlesense.domain.model.FriendshipStatus;
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
		if (friendshipRepository.existsByRequesterUserIdAndAddresseeUserId(requesterUserId, addresseeUserId)
				|| friendshipRepository.existsByRequesterUserIdAndAddresseeUserId(addresseeUserId, requesterUserId)) {
			throw new IllegalArgumentException("friendship already exists for this user pair");
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
}
