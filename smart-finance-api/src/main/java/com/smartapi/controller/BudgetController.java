package com.smartapi.controller;

import com.smartapi.dto.BudgetRequest;
import com.smartapi.dto.BudgetStatus;
import com.smartapi.model.AuditLog;
import com.smartapi.service.BudgetService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/budgets")
public class BudgetController {

    private final BudgetService budgetService;

    public BudgetController(BudgetService budgetService) {
        this.budgetService = budgetService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BudgetStatus define(@Valid @RequestBody BudgetRequest request) {
        budgetService.define(request.category(), request.monthlyLimit(), AuditLog.Source.API);
        return budgetService.evaluate(request.category());
    }

    @GetMapping
    public List<BudgetStatus> list() {
        return budgetService.list().stream()
                .map(b -> budgetService.evaluate(b.getCategory()))
                .toList();
    }

    @GetMapping("/{category}/status")
    public BudgetStatus status(@PathVariable String category) {
        return budgetService.evaluate(category);
    }
}
