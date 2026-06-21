package com.kelvin.settlesense.api;

import java.time.Instant;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kelvin.settlesense.domain.model.ActivityEvent;
import com.kelvin.settlesense.domain.model.dto.ActivityResponse;
import com.kelvin.settlesense.domain.repository.ActivityEventRepository;

@RestController
@RequestMapping("/api")
class ActivityController {

	private final ActivityEventRepository activityEventRepository;

	ActivityController(ActivityEventRepository activityEventRepository) {
		this.activityEventRepository = activityEventRepository;
	}

	@GetMapping("/groups/{groupId}/activity")
	List<ActivityResponse> groupActivity(@PathVariable Long groupId) {
		return activityEventRepository.findByGroupIdOrderByIdAsc(groupId).stream()
				.map(ActivityResponse::from)
				.toList();
	}
}
