package com.smartapi.dto;

import java.math.BigDecimal;

public record BalanceResponse(BigDecimal income, BigDecimal expense, BigDecimal balance) {
}
