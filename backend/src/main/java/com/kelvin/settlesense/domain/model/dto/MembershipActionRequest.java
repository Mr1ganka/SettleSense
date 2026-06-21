package com.kelvin.settlesense.domain.model.dto;

import jakarta.validation.constraints.NotNull;

public record MembershipActionRequest(@NotNull Long actorUserId) {
}
