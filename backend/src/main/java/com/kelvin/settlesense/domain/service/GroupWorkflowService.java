package com.kelvin.settlesense.domain.service;

import java.time.Clock;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.kelvin.settlesense.exceptions.GroupUpdateException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kelvin.settlesense.domain.model.Group;
import com.kelvin.settlesense.domain.model.GroupMember;
import com.kelvin.settlesense.domain.model.GroupMemberRole;
import com.kelvin.settlesense.domain.model.GroupMemberStatus;
import com.kelvin.settlesense.domain.model.GroupStatus;
import com.kelvin.settlesense.domain.model.UserStatus;
import com.kelvin.settlesense.domain.repository.GroupMemberRepository;
import com.kelvin.settlesense.domain.repository.GroupRepository;
import com.kelvin.settlesense.domain.repository.UserRepository;

@Service
public class GroupWorkflowService {

	private final GroupRepository groupRepository;
	private final GroupMemberRepository groupMemberRepository;
	private final UserRepository userRepository;
	private final Clock clock;

	public GroupWorkflowService(GroupRepository groupRepository, GroupMemberRepository groupMemberRepository,
			UserRepository userRepository, Clock clock) {
		this.groupRepository = groupRepository;
		this.groupMemberRepository = groupMemberRepository;
		this.userRepository = userRepository;
		this.clock = clock;
	}

	@Transactional
	public Group createGroup(CreateGroupCommand command) {
		var creator = userRepository.findById(command.createdByUserId())
				.orElseThrow(() -> new IllegalArgumentException("creator user not found"));
		if (creator.getStatus() != UserStatus.ACTIVE) {
			throw new IllegalArgumentException("creator user must be active");
		}

		var now = clock.instant();
		var group = new Group();
		group.setName(requireText(command.name(), "name"));
		group.setCurrencyCode(MoneyRules.normalizeCurrencyCode(command.currencyCode()));
		group.setStatus(GroupStatus.ACTIVE);
		group.setCreatedByUserId(command.createdByUserId());
		group.setCreatedAt(now);
		group.setUpdatedAt(now);
		var savedGroup = groupRepository.save(group);

		var owner = new GroupMember();
		owner.setGroupId(savedGroup.getId());
		owner.setUserId(command.createdByUserId());
		owner.setRole(GroupMemberRole.OWNER);
		owner.setStatus(GroupMemberStatus.ACTIVE);
		owner.setJoinedAt(now);
		groupMemberRepository.save(owner);
		return savedGroup;
	}

	@Transactional
	public GroupMember addMember(AddGroupMemberCommand command) {
		var now = clock.instant();
		requireActiveGroup(command.groupId());
		requireActiveOwner(command.groupId(), command.actorUserId());
		requireActiveUser(command.userId(), "member user");
		if (groupMemberRepository.existsByGroupIdAndUserId(command.groupId(), command.userId())) {
			throw new IllegalArgumentException("user is already associated with this group");
		}

		var member = new GroupMember();
		member.setGroupId(command.groupId());
		member.setUserId(command.userId());
		member.setRole(command.role() == null ? GroupMemberRole.MEMBER : command.role());
		member.setStatus(GroupMemberStatus.ACTIVE);
		member.setJoinedAt(now);
		return groupMemberRepository.save(member);
	}

	@Transactional
	public GroupMember leaveGroup(Long groupId, Long userId, Long actorUserId) {
		if (!userId.equals(actorUserId)) {
			requireActiveOwner(groupId, actorUserId);
		}
		return endMembership(groupId, userId, GroupMemberStatus.LEFT);
	}

	@Transactional
	public GroupMember removeMember(Long groupId, Long userId, Long actorUserId) {
		requireActiveOwner(groupId, actorUserId);
		return endMembership(groupId, userId, GroupMemberStatus.REMOVED);
	}

	@Transactional
	public Group archiveGroup(Long groupId, Long actorUserId) {
		requireActiveOwner(groupId, actorUserId);
		var group = groupRepository.findById(groupId)
				.orElseThrow(() -> new IllegalArgumentException("group not found"));
		if (group.getStatus() == GroupStatus.ARCHIVED) {
			throw new IllegalArgumentException("group is already archived");
		}
		group.setStatus(GroupStatus.ARCHIVED);
		group.setUpdatedAt(clock.instant());
		return groupRepository.save(group);
	}

	@Transactional(readOnly = true)
	public List<GroupMember> members(Long groupId) {
		if (!groupRepository.existsById(groupId)) {
			throw new IllegalArgumentException("group not found");
		}
		return groupMemberRepository.findByGroupId(groupId);
	}

	@Transactional(readOnly = true)
	public List<Group> findGroupsForUser(Long userId) {
		List<GroupMember> memberships = groupMemberRepository.findByUserIdAndStatus(userId, GroupMemberStatus.ACTIVE);
		List<Long> groupIds = memberships.stream().map(GroupMember::getGroupId).toList();
		return groupRepository.findAllById(groupIds).stream()
				.filter(g -> g.getStatus() == GroupStatus.ACTIVE)
				.toList();
	}


	private GroupMember endMembership(Long groupId, Long userId, GroupMemberStatus status) {
		var member = groupMemberRepository.findByGroupIdAndUserId(groupId, userId)
				.orElseThrow(() -> new IllegalArgumentException("group member not found"));
		if (member.getStatus() != GroupMemberStatus.ACTIVE) {
			throw new IllegalArgumentException("group member is not active");
		}
		member.setStatus(status);
		member.setLeftAt(clock.instant());
		return groupMemberRepository.save(member);
	}

	private void requireActiveGroup(Long groupId) {
		var group = groupRepository.findById(groupId)
				.orElseThrow(() -> new IllegalArgumentException("group not found"));
		if (group.getStatus() != GroupStatus.ACTIVE) {
			throw new IllegalArgumentException("group must be active");
		}
	}

	private void requireActiveOwner(Long groupId, Long userId) {
		if (!groupMemberRepository.existsByGroupIdAndUserIdAndRoleAndStatus(groupId, userId, GroupMemberRole.OWNER,
				GroupMemberStatus.ACTIVE)) {
			throw new IllegalArgumentException("actor must be an active group owner");
		}
	}

	private void requireActiveUser(Long userId, String label) {
		var user = userRepository.findById(userId)
				.orElseThrow(() -> new IllegalArgumentException(label + " not found"));
		if (user.getStatus() != UserStatus.ACTIVE) {
			throw new IllegalArgumentException(label + " must be active");
		}
	}

	private String requireText(String value, String fieldName) {
		if (value == null || value.trim().isEmpty()) {
			throw new IllegalArgumentException(fieldName + " is required");
		}
		return value.trim();
	}
}
