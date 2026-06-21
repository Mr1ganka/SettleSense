package com.kelvin.settlesense.domain.model.dto;

import com.kelvin.settlesense.domain.model.Group;

public record GroupResponse(Long id, String name, String currencyCode, String status, Long createdByUserId) {

	public static GroupResponse from(Group group) {
		return new GroupResponse(group.getId(), group.getName(), group.getCurrencyCode(), group.getStatus().name(),
				group.getCreatedByUserId());
	}
}
