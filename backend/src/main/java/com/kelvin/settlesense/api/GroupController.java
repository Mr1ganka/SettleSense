package com.kelvin.settlesense.api;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
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
import com.kelvin.settlesense.domain.model.dto.AddMemberRequest;
import com.kelvin.settlesense.domain.model.dto.CreateGroupRequest;
import com.kelvin.settlesense.domain.model.dto.GroupMemberResponse;
import com.kelvin.settlesense.domain.model.dto.GroupResponse;
import com.kelvin.settlesense.domain.model.dto.MembershipActionRequest;
import com.kelvin.settlesense.domain.repository.GroupRepository;
import com.kelvin.settlesense.domain.service.GroupWorkflowService;

import jakarta.validation.Valid;

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
		return GroupResponse.from(groupWorkflowService.createGroup(request.toCommand(currentUserId(request.createdByUserId()))));
	}

	@GetMapping
	List<GroupResponse> groups() {
		Long userId = currentUserId(null);
		if (userId != null) {
			return groupWorkflowService.findGroupsForUser(userId).stream()
					.map(GroupResponse::from)
					.toList();
		}
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
		return GroupMemberResponse.from(groupWorkflowService.addMember(
				request.toCommand(groupId, currentUserId(request.actorUserId()))));
	}

	@PostMapping("/{groupId}/members/{userId}/leave")
	GroupMemberResponse leaveGroup(@PathVariable Long groupId, @PathVariable Long userId,
			@Valid @RequestBody MembershipActionRequest request) {
		return GroupMemberResponse.from(groupWorkflowService.leaveGroup(groupId, userId,
				currentUserId(request.actorUserId())));
	}

	@PostMapping("/{groupId}/members/{userId}/remove")
	GroupMemberResponse removeMember(@PathVariable Long groupId, @PathVariable Long userId,
			@Valid @RequestBody MembershipActionRequest request) {
		return GroupMemberResponse.from(groupWorkflowService.removeMember(groupId, userId,
				currentUserId(request.actorUserId())));
	}

	@PostMapping("/{groupId}/archive")
	GroupResponse archiveGroup(@PathVariable Long groupId, @Valid @RequestBody MembershipActionRequest request) {
		return GroupResponse.from(groupWorkflowService.archiveGroup(groupId, currentUserId(request.actorUserId())));
	}

	private Long currentUserId(Long fallbackUserId) {
		var authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication != null && authentication.getPrincipal() instanceof com.kelvin.settlesense.domain.model.User user
				&& user.getId() != null) {
			return user.getId();
		}
		return fallbackUserId;
	}
}
