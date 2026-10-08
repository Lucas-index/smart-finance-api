package com.smartapi.repository;

import com.smartapi.model.Budget;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BudgetRepository extends JpaRepository<Budget, Long> {

    Optional<Budget> findByUserIdAndCategory(Long userId, String category);

    List<Budget> findByUserId(Long userId);
}
