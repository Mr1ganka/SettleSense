package com.kelvin.settlesense.domain.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kelvin.settlesense.domain.model.BalanceProjection;

public interface BalanceProjectionRepository extends JpaRepository<BalanceProjection, Long> {

	void deleteByGroupId(Long groupId);

	List<BalanceProjection> findByGroupIdOrderByFromUserIdAscToUserIdAsc(Long groupId);
}
