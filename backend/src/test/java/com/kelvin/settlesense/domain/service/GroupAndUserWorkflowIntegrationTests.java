package com.kelvin.settlesense.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.kelvin.settlesense.domain.model.GroupMemberRole;
import com.kelvin.settlesense.domain.model.GroupMemberStatus;
import com.kelvin.settlesense.domain.model.GroupStatus;
import com.kelvin.settlesense.domain.model.User;
import com.kelvin.settlesense.domain.model.UserStatus;
import com.kelvin.settlesense.domain.repository.ActivityEventRepository;
import com.kelvin.settlesense.domain.repository.BalanceProjectionRepository;
import com.kelvin.settlesense.domain.repository.ExpenseRepository;
import com.kelvin.settlesense.domain.repository.ExpenseSplitRepository;
import com.kelvin.settlesense.domain.repository.FriendshipRepository;
import com.kelvin.settlesense.domain.repository.GroupMemberRepository;
import com.kelvin.settlesense.domain.repository.GroupRepository;
import com.kelvin.settlesense.domain.repository.LedgerEntryRepository;
import com.kelvin.settlesense.domain.repository.SettlementRepository;
import com.kelvin.settlesense.domain.repository.UserRepository;

@ActiveProfiles("test")
@SpringBootTest
class GroupAndUserWorkflowIntegrationTests {

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private GroupRepository groupRepository;

	@Autowired
	private GroupMemberRepository groupMemberRepository;

	@Autowired
	private ExpenseRepository expenseRepository;

	@Autowired
	private ExpenseSplitRepository expenseSplitRepository;

	@Autowired
	private SettlementRepository settlementRepository;

	@Autowired
	private LedgerEntryRepository ledgerEntryRepository;

	@Autowired
	private BalanceProjectionRepository balanceProjectionRepository;

	@Autowired
	private ActivityEventRepository activityEventRepository;

	@Autowired
	private FriendshipRepository friendshipRepository;

	@Autowired
	private UserWorkflowService userWorkflowService;

	@Autowired
	private GroupWorkflowService groupWorkflowService;

	private User owner;
	private User member;

	@BeforeEach
	void setUp() {
		balanceProjectionRepository.deleteAll();
		ledgerEntryRepository.deleteAll();
		activityEventRepository.deleteAll();
		expenseSplitRepository.deleteAll();
		expenseRepository.deleteAll();
		settlementRepository.deleteAll();
		friendshipRepository.deleteAll();
		groupMemberRepository.deleteAll();
		groupRepository.deleteAll();
		userRepository.deleteAll();

		owner = userRepository.save(user("Owner", "owner@example.test"));
		member = userRepository.save(user("Member", "member@example.test"));
	}

	@Test
	void userRegistrationNormalizesEmailAndRejectsDuplicates() {
		var registered = userWorkflowService.registerUser(new RegisterUserCommand(" Kelvin ", "KELVIN@example.test"));

		assertThat(registered.getDisplayName()).isEqualTo("Kelvin");
		assertThat(registered.getEmail()).isEqualTo("kelvin@example.test");
		assertThat(registered.getStatus()).isEqualTo(UserStatus.ACTIVE);
		assertThatThrownBy(() -> userWorkflowService.registerUser(new RegisterUserCommand("Other", "kelvin@example.test")))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("user email already exists");
	}

	@Test
	void groupCreationCreatesOwnerMembership() {
		var group = groupWorkflowService.createGroup(new CreateGroupCommand(" Trip ", "inr", owner.getId()));

		assertThat(group.getName()).isEqualTo("Trip");
		assertThat(group.getCurrencyCode()).isEqualTo("INR");
		assertThat(group.getStatus()).isEqualTo(GroupStatus.ACTIVE);
		assertThat(groupMemberRepository.findByGroupId(group.getId()))
				.extracting("userId", "role", "status")
				.containsExactly(org.assertj.core.groups.Tuple.tuple(owner.getId(), GroupMemberRole.OWNER,
						GroupMemberStatus.ACTIVE));
	}

	@Test
	void onlyActiveOwnerCanAddRemoveAndArchive() {
		var group = groupWorkflowService.createGroup(new CreateGroupCommand("Trip", "INR", owner.getId()));
		var added = groupWorkflowService.addMember(new AddGroupMemberCommand(group.getId(), member.getId(), owner.getId(),
				GroupMemberRole.MEMBER));

		assertThat(added.getStatus()).isEqualTo(GroupMemberStatus.ACTIVE);
		assertThatThrownBy(() -> groupWorkflowService.addMember(new AddGroupMemberCommand(group.getId(), member.getId(),
				owner.getId(), GroupMemberRole.MEMBER)))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("already associated");

		var removed = groupWorkflowService.removeMember(group.getId(), member.getId(), owner.getId());
		assertThat(removed.getStatus()).isEqualTo(GroupMemberStatus.REMOVED);

		var archived = groupWorkflowService.archiveGroup(group.getId(), owner.getId());
		assertThat(archived.getStatus()).isEqualTo(GroupStatus.ARCHIVED);
		assertThatThrownBy(() -> groupWorkflowService.addMember(new AddGroupMemberCommand(group.getId(), member.getId(),
				owner.getId(), GroupMemberRole.MEMBER)))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("group must be active");
	}

	@Test
	void memberCanLeaveSelfButCannotRemoveOthers() {
		var group = groupWorkflowService.createGroup(new CreateGroupCommand("Trip", "INR", owner.getId()));
		groupWorkflowService.addMember(new AddGroupMemberCommand(group.getId(), member.getId(), owner.getId(),
				GroupMemberRole.MEMBER));

		assertThatThrownBy(() -> groupWorkflowService.removeMember(group.getId(), owner.getId(), member.getId()))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("active group owner");

		var left = groupWorkflowService.leaveGroup(group.getId(), member.getId(), member.getId());
		assertThat(left.getStatus()).isEqualTo(GroupMemberStatus.LEFT);
	}

	private User user(String displayName, String email) {
		var now = Instant.parse("2026-06-01T00:00:00Z");
		var user = new User();
		user.setDisplayName(displayName);
		user.setEmail(email);
		user.setStatus(UserStatus.ACTIVE);
		user.setCreatedAt(now);
		user.setUpdatedAt(now);
		return user;
	}
}
