package com.kelvin.settlesense.domain.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kelvin.settlesense.domain.model.ExpenseSplit;

public interface ExpenseSplitRepository extends JpaRepository<ExpenseSplit, Long> {

	List<ExpenseSplit> findByExpenseIdOrderByIdAsc(Long expenseId);

	void deleteByExpenseId(Long expenseId);
}

