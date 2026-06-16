package com.kelvin.settlesense.domain.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kelvin.settlesense.domain.model.GroupMember;
import com.kelvin.settlesense.domain.model.GroupMemberStatus;

public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> {

	boolean existsByGroupIdAndUserIdAndStatus(Long groupId, Long userId, GroupMemberStatus status);

	List<GroupMember> findByGroupId(Long groupId);

	Optional<GroupMember> findByGroupIdAndUserId(Long groupId, Long userId);

	boolean existsByGroupIdAndUserId(Long groupId, Long userId);

	boolean existsByGroupIdAndUserIdAndRoleAndStatus(Long groupId, Long userId,
			com.kelvin.settlesense.domain.model.GroupMemberRole role, GroupMemberStatus status);
}
