package com.kelvin.settlesense.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.kelvin.settlesense.domain.model.BalanceProjection;
import com.kelvin.settlesense.domain.model.Expense;
import com.kelvin.settlesense.domain.model.ExpenseStatus;
import com.kelvin.settlesense.domain.model.Group;
import com.kelvin.settlesense.domain.model.GroupMember;
import com.kelvin.settlesense.domain.model.GroupMemberRole;
import com.kelvin.settlesense.domain.model.GroupMemberStatus;
import com.kelvin.settlesense.domain.model.GroupStatus;
import com.kelvin.settlesense.domain.model.Settlement;
import com.kelvin.settlesense.domain.model.SettlementStatus;
import com.kelvin.settlesense.domain.model.SplitType;
import com.kelvin.settlesense.domain.model.User;
import com.kelvin.settlesense.domain.model.UserStatus;
import com.kelvin.settlesense.domain.repository.BalanceProjectionRepository;
import com.kelvin.settlesense.domain.repository.GroupRepository;
import com.kelvin.settlesense.domain.repository.UserRepository;
import com.kelvin.settlesense.domain.service.AddGroupMemberCommand;
import com.kelvin.settlesense.domain.service.BalanceProjectionService;
import com.kelvin.settlesense.domain.service.CreateGroupCommand;
import com.kelvin.settlesense.domain.service.ExpenseWorkflowService;
import com.kelvin.settlesense.domain.service.GroupWorkflowService;
import com.kelvin.settlesense.domain.service.PostExpenseCommand;
import com.kelvin.settlesense.domain.service.PostSettlementCommand;
import com.kelvin.settlesense.domain.service.RegisterUserCommand;
import com.kelvin.settlesense.domain.service.SettlementWorkflowService;
import com.kelvin.settlesense.domain.service.SimplifiedSettlement;
import com.kelvin.settlesense.domain.service.UserWorkflowService;

class PhaseOneControllerTests {

	@Test
	void expenseControllerPostsExpenseThroughWorkflow() {
		var workflow = mock(ExpenseWorkflowService.class);
		when(workflow.postExpense(any(PostExpenseCommand.class))).thenReturn(expense());
		var controller = new ExpenseController(workflow);

		var response = controller.postExpense(10L, new ExpenseController.PostExpenseRequest(1L, "Dinner", 100000,
				LocalDate.parse("2026-06-01"), 1L, SplitType.EQUAL, Map.of(1L, BigDecimal.ONE, 2L, BigDecimal.ONE)));

		assertThat(response.id()).isEqualTo(100L);
		assertThat(response.status()).isEqualTo("POSTED");
		verify(workflow).postExpense(new PostExpenseCommand(10L, 1L, "Dinner", 100000,
				LocalDate.parse("2026-06-01"), 1L, SplitType.EQUAL, Map.of(1L, BigDecimal.ONE, 2L, BigDecimal.ONE)));
	}

	@Test
	void settlementControllerPostsSettlementThroughWorkflow() {
		var workflow = mock(SettlementWorkflowService.class);
		when(workflow.postSettlement(any(PostSettlementCommand.class))).thenReturn(settlement());
		var controller = new SettlementController(workflow);

		var response = controller.postSettlement(10L, new SettlementController.PostSettlementRequest(2L, 1L, 50000,
				LocalDate.parse("2026-06-02"), 2L));

		assertThat(response.id()).isEqualTo(200L);
		assertThat(response.status()).isEqualTo("POSTED");
		verify(workflow).postSettlement(new PostSettlementCommand(10L, 2L, 1L, 50000,
				LocalDate.parse("2026-06-02"), 2L));
	}

	@Test
	void balanceControllerReturnsBalancesAndSuggestions() {
		var repository = mock(BalanceProjectionRepository.class);
		var service = mock(BalanceProjectionService.class);
		var projection = BalanceProjection.of(10L, 2L, 1L, "INR", 50000, Instant.parse("2026-06-01T00:00:00Z"));
		when(repository.findByGroupIdOrderByFromUserIdAscToUserIdAsc(10L)).thenReturn(List.of(projection));
		when(service.suggestSimplifiedSettlements(List.of(projection)))
				.thenReturn(List.of(new SimplifiedSettlement(2L, 1L, "INR", 50000)));
		var controller = new BalanceController(repository, service);

		assertThat(controller.balances(10L))
				.extracting("fromUserId", "toUserId", "amountMinor")
				.containsExactly(org.assertj.core.groups.Tuple.tuple(2L, 1L, 50000L));
		assertThat(controller.settlementSuggestions(10L))
				.extracting("fromUserId", "toUserId", "amountMinor")
				.containsExactly(org.assertj.core.groups.Tuple.tuple(2L, 1L, 50000L));
	}

	@Test
	void userControllerRegistersUserThroughWorkflow() {
		var workflow = mock(UserWorkflowService.class);
		var repository = mock(UserRepository.class);
		when(workflow.registerUser(any(RegisterUserCommand.class))).thenReturn(user());
		var controller = new UserController(workflow, repository);

		var response = controller.registerUser(new UserController.RegisterUserRequest("Kelvin", "kelvin@example.test"));

		assertThat(response.id()).isEqualTo(1L);
		assertThat(response.status()).isEqualTo("ACTIVE");
		verify(workflow).registerUser(new RegisterUserCommand("Kelvin", "kelvin@example.test"));
	}

	@Test
	void groupControllerCreatesGroupAndAddsMembersThroughWorkflow() {
		var workflow = mock(GroupWorkflowService.class);
		var repository = mock(GroupRepository.class);
		when(workflow.createGroup(any(CreateGroupCommand.class))).thenReturn(group());
		when(workflow.addMember(any(AddGroupMemberCommand.class))).thenReturn(groupMember());
		var controller = new GroupController(workflow, repository);

		var groupResponse = controller.createGroup(new GroupController.CreateGroupRequest("Trip", "inr", 1L));
		var memberResponse = controller.addMember(10L,
				new GroupController.AddMemberRequest(2L, 1L, GroupMemberRole.MEMBER));

		assertThat(groupResponse.id()).isEqualTo(10L);
		assertThat(groupResponse.currencyCode()).isEqualTo("INR");
		assertThat(memberResponse.userId()).isEqualTo(2L);
		verify(workflow).createGroup(new CreateGroupCommand("Trip", "inr", 1L));
		verify(workflow).addMember(new AddGroupMemberCommand(10L, 2L, 1L, GroupMemberRole.MEMBER));
	}

	private Expense expense() {
		var expense = new Expense();
		expense.setId(100L);
		expense.setGroupId(10L);
		expense.setPaidByUserId(1L);
		expense.setDescription("Dinner");
		expense.setCurrencyCode("INR");
		expense.setTotalMinor(100000);
		expense.setExpenseDate(LocalDate.parse("2026-06-01"));
		expense.setStatus(ExpenseStatus.POSTED);
		expense.setCreatedByUserId(1L);
		expense.setCreatedAt(Instant.parse("2026-06-01T00:00:00Z"));
		expense.setUpdatedAt(Instant.parse("2026-06-01T00:00:00Z"));
		return expense;
	}

	private Settlement settlement() {
		var settlement = new Settlement();
		settlement.setId(200L);
		settlement.setGroupId(10L);
		settlement.setFromUserId(2L);
		settlement.setToUserId(1L);
		settlement.setCurrencyCode("INR");
		settlement.setAmountMinor(50000);
		settlement.setSettlementDate(LocalDate.parse("2026-06-02"));
		settlement.setStatus(SettlementStatus.POSTED);
		settlement.setCreatedByUserId(2L);
		settlement.setCreatedAt(Instant.parse("2026-06-01T00:00:00Z"));
		settlement.setUpdatedAt(Instant.parse("2026-06-01T00:00:00Z"));
		return settlement;
	}

	private User user() {
		var user = new User();
		user.setId(1L);
		user.setDisplayName("Kelvin");
		user.setEmail("kelvin@example.test");
		user.setStatus(UserStatus.ACTIVE);
		user.setCreatedAt(Instant.parse("2026-06-01T00:00:00Z"));
		user.setUpdatedAt(Instant.parse("2026-06-01T00:00:00Z"));
		return user;
	}

	private Group group() {
		var group = new Group();
		group.setId(10L);
		group.setName("Trip");
		group.setCurrencyCode("INR");
		group.setStatus(GroupStatus.ACTIVE);
		group.setCreatedByUserId(1L);
		group.setCreatedAt(Instant.parse("2026-06-01T00:00:00Z"));
		group.setUpdatedAt(Instant.parse("2026-06-01T00:00:00Z"));
		return group;
	}

	private GroupMember groupMember() {
		var member = new GroupMember();
		member.setId(20L);
		member.setGroupId(10L);
		member.setUserId(2L);
		member.setRole(GroupMemberRole.MEMBER);
		member.setStatus(GroupMemberStatus.ACTIVE);
		member.setJoinedAt(Instant.parse("2026-06-01T00:00:00Z"));
		return member;
	}
}
