package com.kelvin.settlesense.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kelvin.settlesense.domain.model.Expense;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {
}
