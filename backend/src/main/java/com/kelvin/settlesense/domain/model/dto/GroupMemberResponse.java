package com.kelvin.settlesense.domain.model.dto;

import com.kelvin.settlesense.domain.model.GroupMember;

public record GroupMemberResponse(Long id, Long groupId, Long userId, String role, String status) {

	public static GroupMemberResponse from(GroupMember member) {
		return new GroupMemberResponse(member.getId(), member.getGroupId(), member.getUserId(),
				member.getRole().name(), member.getStatus().name());
	}
}
