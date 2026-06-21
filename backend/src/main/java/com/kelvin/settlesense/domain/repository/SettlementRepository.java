package com.kelvin.settlesense.domain.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kelvin.settlesense.domain.model.Settlement;

public interface SettlementRepository extends JpaRepository<Settlement, Long> {

	List<Settlement> findByGroupIdOrderByIdDesc(Long groupId);
}
