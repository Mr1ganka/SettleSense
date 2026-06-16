package com.kelvin.settlesense.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kelvin.settlesense.domain.model.Friendship;

public interface FriendshipRepository extends JpaRepository<Friendship, Long> {

	boolean existsByRequesterUserIdAndAddresseeUserId(Long requesterUserId, Long addresseeUserId);
}
