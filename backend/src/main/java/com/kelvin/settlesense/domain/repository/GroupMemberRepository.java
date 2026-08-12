package com.kelvin.settlesense.domain.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kelvin.settlesense.domain.model.GroupMember;
import com.kelvin.settlesense.domain.model.GroupMemberRole;
import com.kelvin.settlesense.domain.model.GroupMemberStatus;

public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> {

	boolean existsByGroupIdAndUserIdAndStatus(Long groupId, Long userId, GroupMemberStatus status);

	List<GroupMember> findByGroupId(Long groupId);

	List<GroupMember> findByUserId(Long userId);

	List<GroupMember> findByUserIdAndStatus(Long userId, GroupMemberStatus status);

	Optional<GroupMember> findByGroupIdAndUserId(Long groupId, Long userId);

	boolean existsByGroupIdAndUserId(Long groupId, Long userId);

	boolean existsByGroupIdAndUserIdAndRoleAndStatus(Long groupId, Long userId, GroupMemberRole role, GroupMemberStatus status);
}
