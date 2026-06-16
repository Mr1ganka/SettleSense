package com.kelvin.settlesense.domain.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kelvin.settlesense.domain.model.Group;

public interface GroupRepository extends JpaRepository<Group, Long> {

	List<Group> findAllByOrderByIdAsc();
}
