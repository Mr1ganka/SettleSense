package com.kelvin.settlesense.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kelvin.settlesense.domain.model.InsightRequest;

public interface InsightRequestRepository extends JpaRepository<InsightRequest, Long> {
}
