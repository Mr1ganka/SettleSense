package com.kelvin.settlesense.domain.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.kelvin.settlesense.domain.model.Friendship;
import com.kelvin.settlesense.domain.model.FriendshipStatus;

public interface FriendshipRepository extends JpaRepository<Friendship, Long> {

	boolean existsByRequesterUserIdAndAddresseeUserId(Long requesterUserId, Long addresseeUserId);

	@Query("SELECT f FROM Friendship f WHERE (f.requesterUserId = :userId OR f.addresseeUserId = :userId) AND f.status = :status")
	List<Friendship> findByUserIdAndStatus(@Param("userId") Long userId, @Param("status") FriendshipStatus status);

	@Query("SELECT f FROM Friendship f WHERE f.requesterUserId = :userId OR f.addresseeUserId = :userId")
	List<Friendship> findByUserId(@Param("userId") Long userId);

	@Query("SELECT f FROM Friendship f WHERE (f.requesterUserId = :user1Id AND f.addresseeUserId = :user2Id) OR (f.requesterUserId = :user2Id AND f.addresseeUserId = :user1Id)")
	Optional<Friendship> findFriendshipBetween(@Param("user1Id") Long user1Id, @Param("user2Id") Long user2Id);
}
