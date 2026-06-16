package com.kelvin.settlesense.domain.service;

public record CreateGroupCommand(String name, String currencyCode, Long createdByUserId) {
}
