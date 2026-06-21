package com.kelvin.settlesense.domain.model.dto;

import jakarta.validation.constraints.NotNull;

public record CancelMoneyActionRequest(@NotNull Long actorUserId, String reason) {
}
