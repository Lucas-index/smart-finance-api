package com.smartapi.ai;

import com.smartapi.exception.AiErrors;
import com.smartapi.exception.AssistantException;
import com.smartapi.model.AuditLog;
import com.smartapi.security.CurrentUser;
import com.smartapi.service.AuditService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

/**
 * Orquestra o fluxo do assistente financeiro: mensagem do usuário -> ChatClient (com memória)
 * -> tools (registrar, consultar saldo, checar orçamento) -> resposta em linguagem natural.
 */
@Service
public class AssistantService {

    // Atenção: este texto é tratado como template pelo Spring AI, então evite usar chaves.
    private static final String SYSTEM_PROMPT = """
            Você é o assistente financeiro de uma API de controle de gastos. Responda sempre em português do Brasil, \
            de forma curta e objetiva.

            Regras do fluxo de orçamento:
            1. Quando o usuário relatar um gasto ou ganho, use a tool registerTransaction. Nunca invente valores: \
            se faltar valor ou categoria, pergunte antes.
            2. Depois de registrar uma DESPESA, olhe a situação do orçamento retornada. Se o nível for WARNING \
            ou EXCEEDED, avise o usuário claramente. Se for NO_BUDGET, sugira definir um limite com setBudget.
            3. Para saldo, use getBalance. Para histórico, use listRecentTransactions. Para limites, use checkBudget.
            4. Só afirme que algo foi registrado se a tool retornou sucesso.
            5. Valores em reais (R$). Hoje é %s.
            """;

    private static final Logger log = LoggerFactory.getLogger(AssistantService.class);

    private final ChatClient chatClient;
    private final FinanceTools financeTools;
    private final AuditService auditService;

    public AssistantService(ChatClient chatClient, FinanceTools financeTools, AuditService auditService) {
        this.chatClient = chatClient;
        this.financeTools = financeTools;
        this.auditService = auditService;
    }

    public String chat(String conversationId, String userMessage) {
        return chat(conversationId, userMessage, AuditLog.Source.API);
    }

    public String chat(String conversationId, String userMessage, AuditLog.Source origin) {
        // A memória da conversa é separada por usuário: ninguém enxerga o chat de outra pessoa.
        String memoryId = "user-" + CurrentUser.id() + ":" + conversationId;
        String reply = null;
        for (int attempt = 1; ; attempt++) {
            try {
                reply = chatClient.prompt()
                        .system(SYSTEM_PROMPT.formatted(LocalDate.now()))
                        .user(userMessage)
                        .tools(financeTools)
                        .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, memoryId))
                        .call()
                        .content();
                break;
            } catch (RuntimeException e) {
                // O Llama no Groq às vezes monta a chamada da tool no formato errado; tentar de novo costuma resolver.
                if (attempt < 2 && AiErrors.isToolGlitch(e)) {
                    log.warn("Falha de tool calling na tentativa {}; tentando de novo.", attempt);
                    continue;
                }
                log.error("Falha ao chamar a IA", e);
                throw new AssistantException(AiErrors.explain(e), e);
            }
        }

        auditService.record(origin, "ASSISTANT_CHAT", "conversation=" + conversationId + " | user: " + userMessage);
        return reply;
    }
}
