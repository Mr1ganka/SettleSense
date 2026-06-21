package com.kelvin.settlesense.domain.model.dto;

import java.time.Instant;

public record SystemStatus(String service, String status, Instant timestamp) {
}
