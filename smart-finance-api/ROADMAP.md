# Roadmap e Auditoria — Evoluindo a API Inteligente

## Já implementado
- [x] Spring AI + OpenAI (ChatModel, ChatClient com memória de conversa)
- [x] Tool Calling: registrar transação, listar, saldo, checar/definir orçamento
- [x] Transcription API (Whisper) e Speech API (TTS)
- [x] Fluxo de orçamento: alerta em 80% e estouro em 100% do limite mensal
- [x] Persistência em PostgreSQL via Docker Compose
- [x] REST: `TransactionController`, `BudgetController`, `AssistantController`, `AuditController`
- [x] Auditoria: toda ação (API, tool da IA ou áudio) grava em `audit_logs`

## Próximos passos sugeridos
1. **Segurança**: Spring Security + JWT; associar transações e conversas a um usuário.
2. **Migrations**: trocar `ddl-auto: update` por Flyway.
3. **Memória persistente**: `JdbcChatMemoryRepository` no lugar da memória em RAM.
4. **Streaming**: respostas do chat via SSE (`.stream().content()`).
5. **RAG**: `VectorStore` (pgvector) para responder dúvidas sobre extratos e regras financeiras.
6. **Observabilidade**: Actuator + Micrometer (tokens, latência, custo por conversa).
7. **Guardrails**: limite de valor por transação via IA, confirmação antes de gravar valores altos.
8. **Testes**: testes de integração com Testcontainers + `ChatModel` mockado.
9. **Multi-modelo**: alternar entre OpenAI, Anthropic e Ollama por perfil do Spring.

## Auditoria
Consulte `GET /api/audit` (mais recentes primeiro). Cada registro tem `source`
(`API`, `AI_TOOL`, `AUDIO`), `action`, `details` e `createdAt`.
