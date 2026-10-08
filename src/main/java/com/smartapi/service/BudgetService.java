package com.smartapi.service;

import com.smartapi.dto.BudgetStatus;
import com.smartapi.dto.BudgetStatus.Level;
import com.smartapi.model.AuditLog;
import com.smartapi.model.Budget;
import com.smartapi.model.TransactionType;
import com.smartapi.repository.BudgetRepository;
import com.smartapi.repository.TransactionRepository;
import com.smartapi.security.CurrentUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.List;

@Service
public class BudgetService {

    private static final BigDecimal WARNING_THRESHOLD = new BigDecimal("0.80");

    private final BudgetRepository budgetRepository;
    private final TransactionRepository transactionRepository;
    private final AuditService auditService;

    public BudgetService(BudgetRepository budgetRepository,
                         TransactionRepository transactionRepository,
                         AuditService auditService) {
        this.budgetRepository = budgetRepository;
        this.transactionRepository = transactionRepository;
        this.auditService = auditService;
    }

    @Transactional
    public Budget define(String category, BigDecimal monthlyLimit, AuditLog.Source source) {
        String normalized = CategoryNormalizer.normalize(category);
        Long userId = CurrentUser.id();
        Budget budget = budgetRepository.findByUserIdAndCategory(userId, normalized)
                .orElseGet(() -> new Budget(userId, normalized, monthlyLimit));
        budget.setMonthlyLimit(monthlyLimit);
        Budget saved = budgetRepository.save(budget);
        auditService.record(source, "BUDGET_DEFINED", normalized + " = " + monthlyLimit);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<Budget> list() {
        return budgetRepository.findByUserId(CurrentUser.id());
    }

    /** Compara os gastos do mês corrente da categoria com o limite definido. */
    @Transactional(readOnly = true)
    public BudgetStatus evaluate(String category) {
        String normalized = CategoryNormalizer.normalize(category);
        Long userId = CurrentUser.id();
        Budget budget = budgetRepository.findByUserIdAndCategory(userId, normalized).orElse(null);
        if (budget == null) {
            return new BudgetStatus(normalized, Level.NO_BUDGET, null, null, null,
                    "Nenhum orçamento definido para '" + normalized + "'.");
        }

        YearMonth month = YearMonth.now();
        BigDecimal spent = transactionRepository.sumByTypeAndCategoryBetween(
                userId, TransactionType.EXPENSE, normalized, month.atDay(1), month.atEndOfMonth());
        BigDecimal limit = budget.getMonthlyLimit();
        BigDecimal remaining = limit.subtract(spent);
        BigDecimal ratio = spent.divide(limit, 4, RoundingMode.HALF_UP);

        Level level;
        String message;
        if (ratio.compareTo(BigDecimal.ONE) >= 0) {
            level = Level.EXCEEDED;
            message = "Orçamento de '" + normalized + "' estourado: gasto " + spent + " de " + limit + ".";
        } else if (ratio.compareTo(WARNING_THRESHOLD) >= 0) {
            level = Level.WARNING;
            message = "Atenção: '" + normalized + "' já consumiu " + percent(ratio) + "% do orçamento.";
        } else {
            level = Level.OK;
            message = "Dentro do orçamento de '" + normalized + "' (" + percent(ratio) + "% usado).";
        }
        return new BudgetStatus(normalized, level, limit, spent, remaining, message);
    }

    private static String percent(BigDecimal ratio) {
        return ratio.multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP).toPlainString();
    }
}
