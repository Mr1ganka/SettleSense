package com.kelvin.settlesense.domain.model.dto;

import com.kelvin.settlesense.domain.service.CreateGroupCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateGroupRequest(@NotBlank String name, @NotBlank String currencyCode, @NotNull Long createdByUserId) {

	public CreateGroupCommand toCommand(Long actorUserId) {
		return new CreateGroupCommand(name, currencyCode, actorUserId);
	}
}
