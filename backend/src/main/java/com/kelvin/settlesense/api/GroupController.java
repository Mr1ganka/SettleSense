package com.kelvin.settlesense.api;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.kelvin.settlesense.domain.model.Group;
import com.kelvin.settlesense.domain.model.GroupMember;
import com.kelvin.settlesense.domain.model.GroupMemberRole;
import com.kelvin.settlesense.domain.repository.GroupRepository;
import com.kelvin.settlesense.domain.service.AddGroupMemberCommand;
import com.kelvin.settlesense.domain.service.CreateGroupCommand;
import com.kelvin.settlesense.domain.service.GroupWorkflowService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@RestController
@RequestMapping("/api/groups")
class GroupController {

	private final GroupWorkflowService groupWorkflowService;
	private final GroupRepository groupRepository;

	GroupController(GroupWorkflowService groupWorkflowService, GroupRepository groupRepository) {
		this.groupWorkflowService = groupWorkflowService;
		this.groupRepository = groupRepository;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	GroupResponse createGroup(@Valid @RequestBody CreateGroupRequest request) {
		return GroupResponse.from(groupWorkflowService.createGroup(request.toCommand()));
	}

	@GetMapping
	List<GroupResponse> groups() {
		return groupRepository.findAllByOrderByIdAsc().stream()
				.map(GroupResponse::from)
				.toList();
	}

	@GetMapping("/{groupId}")
	GroupResponse group(@PathVariable Long groupId) {
		return groupRepository.findById(groupId)
				.map(GroupResponse::from)
				.orElseThrow(() -> new IllegalArgumentException("group not found"));
	}

	@GetMapping("/{groupId}/members")
	List<GroupMemberResponse> members(@PathVariable Long groupId) {
		return groupWorkflowService.members(groupId).stream()
				.map(GroupMemberResponse::from)
				.toList();
	}

	@PostMapping("/{groupId}/members")
	@ResponseStatus(HttpStatus.CREATED)
	GroupMemberResponse addMember(@PathVariable Long groupId, @Valid @RequestBody AddMemberRequest request) {
		return GroupMemberResponse.from(groupWorkflowService.addMember(request.toCommand(groupId)));
	}

	@PostMapping("/{groupId}/members/{userId}/leave")
	GroupMemberResponse leaveGroup(@PathVariable Long groupId, @PathVariable Long userId,
			@Valid @RequestBody MembershipActionRequest request) {
		return GroupMemberResponse.from(groupWorkflowService.leaveGroup(groupId, userId, request.actorUserId()));
	}

	@PostMapping("/{groupId}/members/{userId}/remove")
	GroupMemberResponse removeMember(@PathVariable Long groupId, @PathVariable Long userId,
			@Valid @RequestBody MembershipActionRequest request) {
		return GroupMemberResponse.from(groupWorkflowService.removeMember(groupId, userId, request.actorUserId()));
	}

	@PostMapping("/{groupId}/archive")
	GroupResponse archiveGroup(@PathVariable Long groupId, @Valid @RequestBody MembershipActionRequest request) {
		return GroupResponse.from(groupWorkflowService.archiveGroup(groupId, request.actorUserId()));
	}

	record CreateGroupRequest(@NotBlank String name, @NotBlank String currencyCode, @NotNull Long createdByUserId) {
		CreateGroupCommand toCommand() {
			return new CreateGroupCommand(name, currencyCode, createdByUserId);
		}
	}

	record AddMemberRequest(@NotNull Long userId, @NotNull Long actorUserId, GroupMemberRole role) {
		AddGroupMemberCommand toCommand(Long groupId) {
			return new AddGroupMemberCommand(groupId, userId, actorUserId, role);
		}
	}

	record MembershipActionRequest(@NotNull Long actorUserId) {
	}

	record GroupResponse(Long id, String name, String currencyCode, String status, Long createdByUserId) {
		static GroupResponse from(Group group) {
			return new GroupResponse(group.getId(), group.getName(), group.getCurrencyCode(), group.getStatus().name(),
					group.getCreatedByUserId());
		}
	}

	record GroupMemberResponse(Long id, Long groupId, Long userId, String role, String status) {
		static GroupMemberResponse from(GroupMember member) {
			return new GroupMemberResponse(member.getId(), member.getGroupId(), member.getUserId(),
					member.getRole().name(), member.getStatus().name());
		}
	}
}
