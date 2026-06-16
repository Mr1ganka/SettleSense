package com.kelvin.settlesense.api;

import java.time.Instant;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/system")
class SystemController {

	@GetMapping("/status")
	SystemStatus status() {
		return new SystemStatus("settlesense", "ok", Instant.now());
	}

	record SystemStatus(String service, String status, Instant timestamp) {
	}
}
