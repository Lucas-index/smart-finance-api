package com.smartapi.repository;

import com.smartapi.model.Transaction;
import com.smartapi.model.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Page<Transaction> findAllByOrderByDateDescIdDesc(Pageable pageable);

    @Query("select coalesce(sum(t.amount), 0) from Transaction t where t.type = :type")
    BigDecimal sumByType(@Param("type") TransactionType type);

    @Query("""
            select coalesce(sum(t.amount), 0) from Transaction t
            where t.type = :type and t.category = :category
              and t.date between :start and :end
            """)
    BigDecimal sumByTypeAndCategoryBetween(@Param("type") TransactionType type,
                                           @Param("category") String category,
                                           @Param("start") LocalDate start,
                                           @Param("end") LocalDate end);
}
