package com.smartapi.exception;

import java.util.Locale;

/** Traduz erros do provedor de IA em mensagens claras para o usuário (sem vazar chaves). */
public final class AiErrors {

    private AiErrors() {
    }

    /** O modelo montou mal a chamada da função (erro conhecido do Groq com Llama): vale tentar de novo. */
    public static boolean isToolGlitch(Throwable t) {
        String m = fullMessage(t).toLowerCase(Locale.ROOT);
        return m.contains("tool_use_failed") || m.contains("failed to call a function");
    }

    public static String explain(Throwable t) {
        String raw = fullMessage(t);
        String low = raw.toLowerCase(Locale.ROOT);

        if (isToolGlitch(t)) {
            return "A IA se confundiu ao chamar uma função do sistema. Envie a mensagem de novo ou reformule, citando valor e categoria.";
        }
        if (low.contains("401") || low.contains("invalid api key") || low.contains("incorrect api key")) {
            return "A chave de IA é inválida ou não foi configurada. Confira GROQ_API_KEY (ou OPENAI_API_KEY) e reinicie a API.";
        }
        if (low.contains("429") || low.contains("rate limit") || low.contains("quota")) {
            return "Limite de uso da IA atingido. Aguarde alguns minutos e tente de novo.";
        }
        if (low.contains("decommissioned") || low.contains("does not exist") || low.contains("model_not_found")) {
            return "O modelo de IA configurado não está disponível. Troque o modelo no arquivo application-groq.yml.";
        }
        if (low.contains("unknownhost") || low.contains("connect") || low.contains("timed out")) {
            return "Não consegui falar com o serviço de IA. Verifique sua internet.";
        }
        Throwable root = root(t);
        String detail = root.getMessage() != null ? root.getMessage() : root.getClass().getSimpleName();
        detail = detail.replaceAll("(gsk_|sk-)[A-Za-z0-9_\\-*]+", "***");
        if (detail.length() > 300) {
            detail = detail.substring(0, 300) + "...";
        }
        return "Falha ao falar com a IA: " + detail;
    }

    private static String fullMessage(Throwable t) {
        Throwable root = root(t);
        return String.valueOf(t.getMessage()) + " | " + String.valueOf(root.getMessage());
    }

    private static Throwable root(Throwable t) {
        Throwable r = t;
        while (r.getCause() != null && r.getCause() != r) {
            r = r.getCause();
        }
        return r;
    }
}
