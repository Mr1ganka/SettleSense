package com.kelvin.settlesense.domain.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kelvin.settlesense.domain.model.User;

public interface UserRepository extends JpaRepository<User, Long> {

	boolean existsByEmailIgnoreCase(String email);

	List<User> findAllByOrderByIdAsc();

	Optional<User> findByEmail(String email);

	Optional<User> findByDisplayName(String displayName);
}
