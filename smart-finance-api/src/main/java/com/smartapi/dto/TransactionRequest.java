package com.smartapi.dto;

import com.smartapi.model.TransactionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TransactionRequest(
        @NotBlank String description,
        @NotNull @Positive BigDecimal amount,
        @NotNull TransactionType type,
        @NotBlank String category,
        LocalDate date
) {
}
