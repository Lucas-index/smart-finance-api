# Smart Finance API — API Inteligente com Spring AI

API REST de finanças pessoais com assistente de IA: conversa em linguagem natural, registra
gastos via **Tool Calling**, aceita **áudio** (transcrição) e responde em **voz**.

Stack: Java 21 · Spring Boot 3.5 · Spring AI 1.0.3 (OpenAI) · PostgreSQL (Docker) · Maven

## Mapa dos módulos do curso → código

| Módulo | Onde está |
|---|---|
| Spring AI: Setup e Integração com LLMs | `pom.xml`, `application.yml` |
| ChatModel e Modelos de Linguagem | `ai/ChatModelPlayground.java` |
| ChatClient: Fluência e Contexto | `config/AiConfig.java`, `ai/AssistantService.java` |
| Tool Calling | `ai/FinanceTools.java` |
| Transcription API | `ai/TranscriptionService.java` |
| Speech API | `ai/SpeechService.java` |
| Assistente / Fluxo de Budget | `ai/AssistantService.java`, `service/BudgetService.java` |
| Persistência e Docker | `docker-compose.yml`, `model/`, `repository/` |
| Exposição REST | `controller/TransactionController.java` |
| Endpoint de Transcrição | `POST /api/transactions/audio` |
| Roadmap e Auditoria | `ROADMAP.md`, `service/AuditService.java`, `GET /api/audit` |

## Como rodar

Pré-requisitos: JDK 21, Maven 3.9+ (ou use o Maven da extensão Java do VS Code), Docker e uma chave da OpenAI.

```bash
# 1. banco
docker compose up -d

# 2. chave da OpenAI
export OPENAI_API_KEY=sk-...          # Windows PowerShell: $env:OPENAI_API_KEY="sk-..."

# 3. subir a API
mvn spring-boot:run
```

A API sobe em http://localhost:8080. Abra `api.http` no VS Code (extensão **REST Client**)
e clique em *Send Request* em cada exemplo.

## Endpoints

| Método | Rota | Descrição |
|---|---|---|
| POST/GET | `/api/transactions` | criar / listar (paginado) |
| GET/DELETE | `/api/transactions/{id}` | buscar / remover |
| GET | `/api/transactions/balance` | receitas, despesas e saldo |
| POST | `/api/transactions/audio` | multipart `file` (+ `conversationId`): áudio → texto → assistente registra |
| POST/GET | `/api/budgets` | definir / listar orçamentos mensais |
| GET | `/api/budgets/{categoria}/status` | situação do orçamento |
| POST | `/api/assistant/chat` | chat com memória e tools |
| POST | `/api/assistant/chat/voice` | chat com resposta em mp3 |
| POST | `/api/assistant/speech` | texto → mp3 |
| GET | `/api/assistant/playground` | ChatModel direto |
| GET | `/api/audit` | trilha de auditoria |

## Testes

```bash
mvn test
```
