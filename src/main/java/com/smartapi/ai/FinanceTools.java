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
            @ToolParam(description = "Valor positivo em reais, como texto, ex: '45.90'") String amount,
            @ToolParam(description = "Tipo: INCOME (receita) ou EXPENSE (despesa)") String type,
            @ToolParam(description = "Categoria, ex: alimentação, transporte, lazer, salário") String category,
            @ToolParam(description = "Data no formato AAAA-MM-DD. Se omitida, usa hoje", required = false) String date) {

        TransactionType parsedType = parseType(type);
        LocalDate parsedDate = (date == null || date.isBlank()) ? LocalDate.now() : LocalDate.parse(date.trim());
        return transactionService.create(
                new TransactionRequest(description, parseMoney(amount), parsedType, category, parsedDate),
                AuditLog.Source.AI_TOOL);
    }

    @Tool(description = "Lista as transações mais recentes do usuário.")
    public List<TransactionResponse> listRecentTransactions(
            @ToolParam(description = "Quantidade de transações, de 1 a 20, como texto, ex: '5'", required = false) String limit) {
        int size = 5;
        if (limit != null && !limit.isBlank()) {
            try {
                size = Math.max(1, Math.min(20, new BigDecimal(limit.trim()).intValue()));
            } catch (NumberFormatException ignored) {
                // mantém o padrão
            }
        }
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
            @ToolParam(description = "Limite mensal em reais, valor positivo, como texto, ex: '400'") String monthlyLimit) {
        budgetService.define(category, parseMoney(monthlyLimit), AuditLog.Source.AI_TOOL);
        return budgetService.evaluate(category);
    }

    // ---------- conversões tolerantes ----------

    static TransactionType parseType(String raw) {
        String t = raw == null ? "" : raw.trim().toUpperCase();
        return switch (t) {
            case "INCOME", "RECEITA", "ENTRADA", "GANHO" -> TransactionType.INCOME;
            case "EXPENSE", "DESPESA", "GASTO", "SAIDA", "SAÍDA" -> TransactionType.EXPENSE;
            default -> throw new IllegalArgumentException("Tipo inválido: use INCOME ou EXPENSE.");
        };
    }

    /** Aceita "400", "45.90", "45,90", "R$ 1.234,56" e "1,234.56". */
    static BigDecimal parseMoney(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Valor é obrigatório.");
        }
        String s = raw.replaceAll("[^0-9.,]", "");
        if (s.isEmpty()) {
            throw new IllegalArgumentException("Valor inválido: " + raw);
        }
        int lastDot = s.lastIndexOf('.');
        int lastComma = s.lastIndexOf(',');
        if (lastDot >= 0 && lastComma >= 0) {
            if (lastComma > lastDot) {            // 1.234,56
                s = s.replace(".", "").replace(',', '.');
            } else {                              // 1,234.56
                s = s.replace(",", "");
            }
        } else if (lastComma >= 0) {              // 45,90  ou  1,234
            s = s.matches("\\d{1,3}(,\\d{3})+") ? s.replace(",", "") : s.replace(',', '.');
        } else if (lastDot >= 0 && s.matches("\\d{1,3}(\\.\\d{3})+")) {   // 1.234 (milhar)
            s = s.replace(".", "");
        }
        BigDecimal value = new BigDecimal(s);
        if (value.signum() <= 0) {
            throw new IllegalArgumentException("O valor deve ser maior que zero.");
        }
        return value;
    }
}