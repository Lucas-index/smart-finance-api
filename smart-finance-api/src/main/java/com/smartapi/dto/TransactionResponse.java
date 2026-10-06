package com.smartapi.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.smartapi.model.Transaction;
import com.smartapi.model.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record TransactionResponse(
        Long id,
        String description,
        BigDecimal amount,
        TransactionType type,
        String category,
        LocalDate date,
        BudgetStatus budget
) {
    public static TransactionResponse from(Transaction t) {
        return from(t, null);
    }

    public static TransactionResponse from(Transaction t, BudgetStatus budget) {
        return new TransactionResponse(t.getId(), t.getDescription(), t.getAmount(),
                t.getType(), t.getCategory(), t.getDate(), budget);
    }
}
