package com.smartapi.service;

import com.smartapi.dto.BudgetStatus;
import com.smartapi.dto.BudgetStatus.Level;
import com.smartapi.model.Budget;
import com.smartapi.model.TransactionType;
import com.smartapi.repository.BudgetRepository;
import com.smartapi.repository.TransactionRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BudgetServiceTest {

    private final BudgetRepository budgets = mock(BudgetRepository.class);
    private final TransactionRepository transactions = mock(TransactionRepository.class);
    private final BudgetService service = new BudgetService(budgets, transactions, mock(AuditService.class));

    private void givenBudget(String category, String limit, String spent) {
        when(budgets.findByCategory(category)).thenReturn(Optional.of(new Budget(category, new BigDecimal(limit))));
        when(transactions.sumByTypeAndCategoryBetween(
                eq(TransactionType.EXPENSE), eq(category), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(new BigDecimal(spent));
    }

    @Test
    void semOrcamentoRetornaNoBudget() {
        when(budgets.findByCategory("lazer")).thenReturn(Optional.empty());
        assertEquals(Level.NO_BUDGET, service.evaluate("Lazer").level());
    }

    @Test
    void abaixoDe80PorCentoEstaOk() {
        givenBudget("lazer", "500", "100");
        assertEquals(Level.OK, service.evaluate("lazer").level());
    }

    @Test
    void a80PorCentoGeraAlerta() {
        givenBudget("lazer", "500", "400");
        assertEquals(Level.WARNING, service.evaluate("lazer").level());
    }

    @Test
    void acimaDoLimiteEstoura() {
        givenBudget("lazer", "500", "550");
        BudgetStatus status = service.evaluate("lazer");
        assertEquals(Level.EXCEEDED, status.level());
        assertEquals(new BigDecimal("-50"), status.remaining());
    }
}
