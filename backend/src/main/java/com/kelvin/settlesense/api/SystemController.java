package com.kelvin.settlesense.api;

import java.time.Instant;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kelvin.settlesense.domain.model.dto.SystemStatus;

@RestController
@RequestMapping("/api/system")
class SystemController {

	@GetMapping("/status")
	SystemStatus status() {
		return new SystemStatus("settlesense", "ok", Instant.now());
	}
}
