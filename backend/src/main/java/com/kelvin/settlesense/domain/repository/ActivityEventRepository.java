package com.kelvin.settlesense.domain.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kelvin.settlesense.domain.model.ActivityEvent;

public interface ActivityEventRepository extends JpaRepository<ActivityEvent, Long> {

	List<ActivityEvent> findByGroupIdOrderByIdAsc(Long groupId);
}
