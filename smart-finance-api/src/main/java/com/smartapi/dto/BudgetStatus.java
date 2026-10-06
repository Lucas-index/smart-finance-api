package com.smartapi.dto;

import java.math.BigDecimal;

/** Situação do orçamento mensal de uma categoria. */
public record BudgetStatus(
        String category,
        Level level,
        BigDecimal limit,
        BigDecimal spent,
        BigDecimal remaining,
        String message
) {
    public enum Level { NO_BUDGET, OK, WARNING, EXCEEDED }
}
