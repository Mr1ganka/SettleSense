package com.kelvin.settlesense.domain.service;

import com.kelvin.settlesense.domain.model.GroupMemberRole;

public record AddGroupMemberCommand(Long groupId, Long userId, Long actorUserId, GroupMemberRole role) {
}
