package com.smartapi.service;

import com.smartapi.dto.BalanceResponse;
import com.smartapi.dto.BudgetStatus;
import com.smartapi.dto.TransactionRequest;
import com.smartapi.dto.TransactionResponse;
import com.smartapi.exception.NotFoundException;
import com.smartapi.model.AuditLog;
import com.smartapi.model.Transaction;
import com.smartapi.model.TransactionType;
import com.smartapi.repository.TransactionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
public class TransactionService {

    private final TransactionRepository repository;
    private final BudgetService budgetService;
    private final AuditService auditService;

    public TransactionService(TransactionRepository repository,
                              BudgetService budgetService,
                              AuditService auditService) {
        this.repository = repository;
        this.budgetService = budgetService;
        this.auditService = auditService;
    }

    @Transactional
    public TransactionResponse create(TransactionRequest request, AuditLog.Source source) {
        String category = CategoryNormalizer.normalize(request.category());
        LocalDate date = request.date() != null ? request.date() : LocalDate.now();

        Transaction saved = repository.save(new Transaction(
                request.description().trim(), request.amount(), request.type(), category, date));

        auditService.record(source, "TRANSACTION_CREATED",
                saved.getType() + " " + saved.getAmount() + " [" + category + "] " + saved.getDescription());

        BudgetStatus budget = request.type() == TransactionType.EXPENSE
                ? budgetService.evaluate(category)
                : null;
        return TransactionResponse.from(saved, budget);
    }

    @Transactional(readOnly = true)
    public Page<TransactionResponse> list(Pageable pageable) {
        return repository.findAllByOrderByDateDescIdDesc(pageable).map(TransactionResponse::from);
    }

    @Transactional(readOnly = true)
    public TransactionResponse get(Long id) {
        return repository.findById(id)
                .map(TransactionResponse::from)
                .orElseThrow(() -> new NotFoundException("Transação " + id + " não encontrada."));
    }

    @Transactional
    public void delete(Long id) {
        Transaction t = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Transação " + id + " não encontrada."));
        repository.delete(t);
        auditService.record(AuditLog.Source.API, "TRANSACTION_DELETED", "id=" + id);
    }

    @Transactional(readOnly = true)
    public BalanceResponse balance() {
        BigDecimal income = repository.sumByType(TransactionType.INCOME);
        BigDecimal expense = repository.sumByType(TransactionType.EXPENSE);
        return new BalanceResponse(income, expense, income.subtract(expense));
    }
}
