package com.kelvin.settlesense.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.kelvin.settlesense.domain.model.Group;
import com.kelvin.settlesense.domain.model.GroupMember;
import com.kelvin.settlesense.domain.model.GroupMemberRole;
import com.kelvin.settlesense.domain.model.GroupMemberStatus;
import com.kelvin.settlesense.domain.model.GroupStatus;
import com.kelvin.settlesense.domain.model.SplitType;
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
class PhaseOneWorkflowIntegrationTests {

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
	private ExpenseWorkflowService expenseWorkflowService;

	@Autowired
	private SettlementWorkflowService settlementWorkflowService;

	@Autowired
	private FriendshipService friendshipService;

	private User userA;
	private User userB;
	private User userC;
	private Group group;

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

		userA = userRepository.save(user("A", "a@example.test"));
		userB = userRepository.save(user("B", "b@example.test"));
		userC = userRepository.save(user("C", "c@example.test"));
		group = groupRepository.save(group(userA.getId()));
		groupMemberRepository.save(member(group.getId(), userA.getId(), GroupMemberRole.OWNER));
		groupMemberRepository.save(member(group.getId(), userB.getId(), GroupMemberRole.MEMBER));
		groupMemberRepository.save(member(group.getId(), userC.getId(), GroupMemberRole.MEMBER));
	}

	@Test
	void postingExpensePersistsSplitsLedgerActivityAndBalanceProjection() {
		var expense = expenseWorkflowService.postExpense(new PostExpenseCommand(group.getId(), userA.getId(), "Dinner",
				100000, LocalDate.parse("2026-06-01"), userA.getId(), SplitType.EQUAL,
				Map.of(userA.getId(), BigDecimal.ONE, userB.getId(), BigDecimal.ONE, userC.getId(), BigDecimal.ONE)));

		assertThat(expenseRepository.findById(expense.getId())).isPresent();
		assertThat(expenseSplitRepository.findByExpenseIdOrderByIdAsc(expense.getId())).hasSize(3);
		assertThat(ledgerEntryRepository.findByGroupIdOrderByIdAsc(group.getId())).hasSize(2);
		assertThat(activityEventRepository.findByGroupIdOrderByIdAsc(group.getId()))
				.extracting("eventType")
				.containsExactly("EXPENSE_POSTED");
		assertThat(balanceProjectionRepository.findByGroupIdOrderByFromUserIdAscToUserIdAsc(group.getId()))
				.extracting("fromUserId", "toUserId", "amountMinor")
				.containsExactlyInAnyOrder(
						org.assertj.core.groups.Tuple.tuple(userB.getId(), userA.getId(), 33333L),
						org.assertj.core.groups.Tuple.tuple(userC.getId(), userA.getId(), 33333L));
	}

	@Test
	void settlementAndCancellationUpdateProjectionFromLedger() {
		var expense = expenseWorkflowService.postExpense(new PostExpenseCommand(group.getId(), userA.getId(), "Dinner",
				100000, LocalDate.parse("2026-06-01"), userA.getId(), SplitType.EXACT,
				Map.of(userA.getId(), new BigDecimal("50000"), userB.getId(), new BigDecimal("50000"))));
		var settlement = settlementWorkflowService.postSettlement(new PostSettlementCommand(group.getId(), userB.getId(),
				userA.getId(), 20000, LocalDate.parse("2026-06-02"), userB.getId()));

		assertThat(balanceProjectionRepository.findByGroupIdOrderByFromUserIdAscToUserIdAsc(group.getId()))
				.extracting("fromUserId", "toUserId", "amountMinor")
				.containsExactly(org.assertj.core.groups.Tuple.tuple(userB.getId(), userA.getId(), 30000L));

		settlementWorkflowService.cancelSettlement(settlement.getId(), userA.getId(), "wrong payment");
		assertThat(balanceProjectionRepository.findByGroupIdOrderByFromUserIdAscToUserIdAsc(group.getId()))
				.extracting("fromUserId", "toUserId", "amountMinor")
				.containsExactly(org.assertj.core.groups.Tuple.tuple(userB.getId(), userA.getId(), 50000L));

		expenseWorkflowService.cancelExpense(expense.getId(), userA.getId(), "duplicate");
		assertThat(balanceProjectionRepository.findByGroupIdOrderByFromUserIdAscToUserIdAsc(group.getId())).isEmpty();
		assertThat(activityEventRepository.findByGroupIdOrderByIdAsc(group.getId()))
				.extracting("eventType")
				.containsExactly("EXPENSE_POSTED", "SETTLEMENT_POSTED", "SETTLEMENT_CANCELLED", "EXPENSE_CANCELLED");
	}

	@Test
	void inactiveMemberCannotBeAddedToNewExpense() {
		var membership = groupMemberRepository.findByGroupId(group.getId()).stream()
				.filter(member -> member.getUserId().equals(userC.getId()))
				.findFirst()
				.orElseThrow();
		membership.setStatus(GroupMemberStatus.LEFT);
		membership.setLeftAt(Instant.parse("2026-06-01T00:00:00Z"));
		groupMemberRepository.save(membership);

		assertThatThrownBy(() -> expenseWorkflowService.postExpense(new PostExpenseCommand(group.getId(), userA.getId(),
				"Dinner", 100000, LocalDate.parse("2026-06-01"), userA.getId(), SplitType.EQUAL,
				Map.of(userA.getId(), BigDecimal.ONE, userB.getId(), BigDecimal.ONE, userC.getId(), BigDecimal.ONE))))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("split participant must be an active group member");
	}

	@Test
	void friendshipDuplicateIsRejectedRegardlessOfDirection() {
		friendshipService.requestFriendship(userA.getId(), userB.getId());

		assertThatThrownBy(() -> friendshipService.requestFriendship(userB.getId(), userA.getId()))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("friendship already exists");
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

	private Group group(Long createdByUserId) {
		var now = Instant.parse("2026-06-01T00:00:00Z");
		var group = new Group();
		group.setName("Trip");
		group.setCurrencyCode("INR");
		group.setStatus(GroupStatus.ACTIVE);
		group.setCreatedByUserId(createdByUserId);
		group.setCreatedAt(now);
		group.setUpdatedAt(now);
		return group;
	}

	private GroupMember member(Long groupId, Long userId, GroupMemberRole role) {
		var member = new GroupMember();
		member.setGroupId(groupId);
		member.setUserId(userId);
		member.setRole(role);
		member.setStatus(GroupMemberStatus.ACTIVE);
		member.setJoinedAt(Instant.parse("2026-06-01T00:00:00Z"));
		return member;
	}
}
