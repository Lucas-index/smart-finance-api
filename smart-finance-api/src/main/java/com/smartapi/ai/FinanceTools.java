package com.smartapi.ai;

import com.smartapi.dto.BalanceResponse;
import com.smartapi.dto.BudgetStatus;
import com.smartapi.dto.TransactionRequest;
import com.smartapi.dto.TransactionResponse;
import com.smartapi.model.AuditLog;
import com.smartapi.model.TransactionType;
import com.smartapi.service.BudgetService;
import com.smartapi.service.TransactionService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Tool Calling: funções reais que o modelo pode chamar para operar o sistema.
 * Cada chamada passa pelos mesmos serviços da API REST (e é auditada como AI_TOOL).
 */
@Component
public class FinanceTools {

    private final TransactionService transactionService;
    private final BudgetService budgetService;

    public FinanceTools(TransactionService transactionService, BudgetService budgetService) {
        this.transactionService = transactionService;
        this.budgetService = budgetService;
    }

    @Tool(description = """
            Registra uma transação financeira (receita ou despesa) do usuário.
            Retorna a transação salva e, para despesas, a situação do orçamento da categoria.""")
    public TransactionResponse registerTransaction(
            @ToolParam(description = "Descrição curta, ex: 'Almoço no restaurante'") String description,
            @ToolParam(description = "Valor positivo em reais, ex: 45.90") double amount,
            @ToolParam(description = "Tipo: INCOME (receita) ou EXPENSE (despesa)") String type,
            @ToolParam(description = "Categoria, ex: alimentação, transporte, lazer, salário") String category,
            @ToolParam(description = "Data no formato AAAA-MM-DD. Se omitida, usa hoje", required = false) String date) {

        TransactionType parsedType = TransactionType.valueOf(type.trim().toUpperCase());
        LocalDate parsedDate = (date == null || date.isBlank()) ? LocalDate.now() : LocalDate.parse(date.trim());
        return transactionService.create(
                new TransactionRequest(description, BigDecimal.valueOf(amount), parsedType, category, parsedDate),
                AuditLog.Source.AI_TOOL);
    }

    @Tool(description = "Lista as transações mais recentes do usuário.")
    public List<TransactionResponse> listRecentTransactions(
            @ToolParam(description = "Quantidade de transações (1 a 20)", required = false) Integer limit) {
        int size = (limit == null || limit < 1) ? 5 : Math.min(limit, 20);
        return transactionService.list(PageRequest.of(0, size)).getContent();
    }

    @Tool(description = "Retorna o saldo atual: total de receitas, total de despesas e saldo.")
    public BalanceResponse getBalance() {
        return transactionService.balance();
    }

    @Tool(description = "Verifica quanto do orçamento mensal de uma categoria já foi gasto no mês atual.")
    public BudgetStatus checkBudget(
            @ToolParam(description = "Categoria, ex: alimentação") String category) {
        return budgetService.evaluate(category);
    }

    @Tool(description = "Define ou atualiza o limite de orçamento mensal de uma categoria.")
    public BudgetStatus setBudget(
            @ToolParam(description = "Categoria, ex: lazer") String category,
            @ToolParam(description = "Limite mensal em reais, valor positivo") double monthlyLimit) {
        budgetService.define(category, BigDecimal.valueOf(monthlyLimit), AuditLog.Source.AI_TOOL);
        return budgetService.evaluate(category);
    }
}
