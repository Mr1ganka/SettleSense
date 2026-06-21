package com.kelvin.settlesense.domain.model.dto;

import com.kelvin.settlesense.domain.model.GroupMemberRole;
import com.kelvin.settlesense.domain.service.AddGroupMemberCommand;
import jakarta.validation.constraints.NotNull;

public record AddMemberRequest(@NotNull Long userId, @NotNull Long actorUserId, GroupMemberRole role) {

	public AddGroupMemberCommand toCommand(Long groupId, Long actorUserId) {
		return new AddGroupMemberCommand(groupId, userId, actorUserId, role);
	}
}
