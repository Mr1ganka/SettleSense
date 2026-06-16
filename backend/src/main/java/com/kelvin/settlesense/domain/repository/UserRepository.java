package com.kelvin.settlesense.domain.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kelvin.settlesense.domain.model.User;

public interface UserRepository extends JpaRepository<User, Long> {

	boolean existsByEmailIgnoreCase(String email);

	List<User> findAllByOrderByIdAsc();
}
