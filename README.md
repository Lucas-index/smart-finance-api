# Smart Finance API — API Inteligente com Spring AI

API REST de **finanças pessoais com assistente de IA**. Você registra receitas e despesas pela API
normal **ou conversando em português** ("gastei 80 reais no mercado"), e a IA grava no banco sozinha,
confere o orçamento da categoria e avisa quando você está perto do limite ou estourou.

---

## Início rápido (3 passos)

**A tela (front-end) e a API são um programa só.** A tela fica dentro do projeto e a própria API a entrega no navegador. Subir a API já sobe a tela junto, não há dois programas para abrir.

| Endereço | O que é |
|---|---|
| `http://localhost:8080` | a **tela** (chat, saldo, transações, orçamentos, histórico) |
| `http://localhost:8080/api/...` | a **API** (usada pela tela por baixo e pelo `api.http`) |
| `http://localhost:8080/h2-console` | o **banco de dados** (para consulta) |

**Opção A — Windows, dois cliques (mais fácil)**
1. Tenha o **JDK 21** instalado (seção 5).
2. Dê dois cliques em **`iniciar.cmd`**. Na primeira vez ele pede a chave do Groq e a guarda em `chave.txt` (só no seu computador).
3. Aguarde cerca de 20 segundos: a tela abre sozinha no navegador. Para encerrar, feche a janela preta do terminal.

**Opção B — pelo terminal (Git Bash)**
```bash
export GROQ_API_KEY=sua_chave                                   # chave gratuita em console.groq.com/keys
./run.cmd spring-boot:run -Dspring-boot.run.profiles=groq       # espere: Started SmartFinanceApiApplication
```
Depois abra **http://localhost:8080** no navegador.

**Opção C — sem chave nenhuma:** `./run.cmd spring-boot:run`. A tela abre, mas só funcionam as partes sem IA (transações manuais, orçamentos, saldo e histórico). O chat e o microfone vão dar erro de autenticação.

---

## 1. O que o projeto faz

| Recurso | Descrição |
|---|---|
| **CRUD de transações** | Criar, listar, buscar e remover receitas/despesas, mais consulta de saldo. |
| **Orçamento mensal por categoria** | Define um limite (ex.: alimentação = R$ 500). A API avisa em **80%** do limite (`WARNING`) e quando **estoura** (`EXCEEDED`). |
| **Assistente com IA (chat)** | Entende linguagem natural, lembra do contexto da conversa e usa **Tool Calling** para executar ações reais no sistema. |
| **Áudio → transação** | Você envia um áudio ("gastei 45 reais no almoço"), ele é transcrito e o assistente registra o gasto. |
| **Texto → voz** | Converte a resposta do assistente (ou qualquer texto) em áudio mp3. |
| **Tela web** | Painel completo em `http://localhost:8080`: chat, gravação de áudio, saldo, transações, orçamentos com barra de progresso e histórico de ações. |
| **Auditoria** | Toda ação (feita pela API, pela IA ou por áudio) fica gravada e pode ser consultada. |

**Stack:** Java 21 · Spring Boot 3.5 · Spring AI 1.0.3 · Spring Data JPA · H2 (banco em arquivo, sem Docker) · Maven

---

## 2. Como funciona (fluxo do assistente)

```
 Usuário: "Gastei 380 reais com jantar fora hoje"
        │
        ▼
 POST /api/assistant/chat ──► AssistantService
                                   │  monta o prompt (regras do sistema + histórico da conversa)
                                   ▼
                              ChatClient (Spring AI) ──► modelo de linguagem (OpenAI ou Groq)
                                   ▲        │
                                   │        │ o modelo decide chamar uma "tool"
                                   │        ▼
                                   │   FinanceTools.registerTransaction(...)   ◄── Tool Calling
                                   │        │
                                   │        ▼
                                   │   TransactionService ──► banco H2 (+ auditoria)
                                   │        │
                                   │        ▼
                                   │   BudgetService: compara gasto do mês x limite
                                   │        │
                                   └────────┘  resultado volta para o modelo
        │
        ▼
 Resposta em português: "Registrei R$ 380,00 em alimentação. Atenção: você já usou 100% do orçamento!"
```

Pontos importantes:
- **Tool Calling:** o modelo não grava nada direto no banco. Ele *pede* para o sistema executar uma função (`registerTransaction`, `getBalance`, `checkBudget`...). Quem executa é o código Java, passando pelas mesmas regras e validações da API REST.
- **Memória:** o `conversationId` identifica a conversa. Usando o mesmo id, a IA lembra do que foi dito antes (últimas 20 mensagens, guardadas em memória RAM — reiniciar a API apaga o histórico do chat, mas **não** as transações).
- **Orçamento:** só despesas contam. O cálculo considera o **mês corrente** e a categoria (comparação em minúsculas, então "Alimentação" e "alimentação" são a mesma coisa).

---

## 3. Mapa dos módulos do curso → código

| Módulo do curso | Onde está no projeto |
|---|---|
| Spring AI: Setup e Integração com LLMs | `pom.xml`, `src/main/resources/application.yml` |
| Explorando o ChatModel | `ai/ChatModelPlayground.java` → `GET /api/assistant/playground` |
| ChatClient: Fluência e Contexto | `config/AiConfig.java` (memória + advisors), `ai/AssistantService.java` |
| Tool Calling | `ai/FinanceTools.java` |
| Transcription API | `ai/TranscriptionService.java` |
| Speech API | `ai/SpeechService.java` |
| Integração do Assistente (fluxo de Budget) | `ai/AssistantService.java`, `service/BudgetService.java` |
| Persistência e Infraestrutura | `model/`, `repository/`, `application.yml` (H2) e `docker-compose.yml` (Postgres opcional) |
| Exposição REST | `controller/TransactionController.java` (+ `BudgetController`, `AssistantController`) |
| Endpoint de Transcrição | `POST /api/transactions/audio` |
| Roadmap e Auditoria | `ROADMAP.md`, `service/AuditService.java`, `GET /api/audit` |

---

## 4. Estrutura de pastas

```
smart-finance-api/
├── pom.xml                      dependências (Spring Boot, Spring AI, JPA, H2)
├── run.cmd                      roda o Maven mesmo sem tê-lo instalado (Windows)
├── iniciar.cmd                  duplo clique: sobe a API e abre a tela (Windows)
├── api.http                     requisições prontas para o VS Code (extensão REST Client)
├── ROADMAP.md                   próximos passos do projeto
├── docker-compose.yml           Postgres opcional (não é necessário)
└── src/main/
    ├── resources/
    │   ├── static/                    tela web (index.html, css/style.css, js/app.js)
    │   ├── application.yml            configuração padrão (OpenAI + H2)
    │   ├── application-groq.yml       perfil gratuito (Groq)
    │   └── application-postgres.yml   perfil opcional (Postgres)
    └── java/com/smartapi/
        ├── ai/            AssistantService, FinanceTools, TranscriptionService, SpeechService, ChatModelPlayground
        ├── config/        AiConfig (ChatClient + memória)
        ├── controller/    TransactionController, BudgetController, AssistantController, AuditController
        ├── service/       TransactionService, BudgetService, AuditService
        ├── model/         Transaction, Budget, AuditLog (entidades JPA)
        ├── repository/    interfaces Spring Data
        ├── dto/           objetos de entrada/saída da API
        └── exception/     tratamento global de erros
```

---

## 5. Pré-requisitos

- **JDK 21** — confira com `java -version`. Para instalar no Windows: `winget install EclipseAdoptium.Temurin.21.JDK` (depois feche e abra o terminal).
- **Maven** — opcional. Se não tiver, use o `run.cmd` do projeto (ele baixa o Maven sozinho na primeira vez).
- **Uma chave de API de IA** — escolha uma:
  - **Groq (gratuito, com limite diário de uso):** crie em https://console.groq.com/keys
  - **OpenAI (pago):** crie em https://platform.openai.com/api-keys
  - **Nenhuma:** a API sobe mesmo assim, mas só os endpoints sem IA funcionam (ver seção 6.3).
- **Docker não é necessário.** O banco H2 cria o arquivo `./data/smartfinance` sozinho.

> **Segurança:** nunca coloque a chave dentro de arquivos do projeto nem a compartilhe em chats ou no Git. Ela é passada por variável de ambiente, só no seu terminal.

---

## 6. Como rodar

Abra o terminal **dentro da pasta do projeto** (no VS Code: *Terminal → New Terminal*).

### 6.1 Com Groq (gratuito)

**Git Bash / Linux / macOS**
```bash
export GROQ_API_KEY=gsk_sua_chave_aqui
./run.cmd spring-boot:run -Dspring-boot.run.profiles=groq     # Windows sem Maven instalado
# ou, se você tem Maven:
mvn spring-boot:run -Dspring-boot.run.profiles=groq
```

**PowerShell**
```powershell
$env:GROQ_API_KEY="gsk_sua_chave_aqui"
.\run.cmd spring-boot:run "-Dspring-boot.run.profiles=groq"
```

**CMD**
```bat
set GROQ_API_KEY=gsk_sua_chave_aqui
run.cmd spring-boot:run -Dspring-boot.run.profiles=groq
```

Neste perfil o chat, a memória e o Tool Calling usam o modelo `llama-3.3-70b-versatile` e a transcrição usa `whisper-large-v3-turbo`.
A síntese de voz (`/speech` e `/chat/voice`) provavelmente **não funciona** no Groq.

### 6.2 Com OpenAI (pago)

```bash
export OPENAI_API_KEY=sk-sua_chave_aqui          # PowerShell: $env:OPENAI_API_KEY="sk-..."
./run.cmd spring-boot:run                        # ou: mvn spring-boot:run
```
Usa `gpt-4o-mini` (chat), `whisper-1` (transcrição) e `tts-1` (voz). Todos os recursos funcionam.

### 6.3 Sem nenhuma chave

```bash
./run.cmd spring-boot:run                        # ou: mvn spring-boot:run
```
Funcionam: `/api/transactions`, `/api/budgets` e `/api/audit`. Os endpoints de chat, áudio e voz retornam erro de autenticação.

### 6.4 Como saber que deu certo

Na **primeira execução** o Maven baixa tudo e pode demorar alguns minutos. Quando aparecer algo como:

```
Started SmartFinanceApiApplication in 6.2 seconds
```

a API está no ar em **http://localhost:8080**. **Deixe esse terminal aberto** (Ctrl+C derruba a API).

### 6.5 Abrir a tela web (uso no dia a dia)

Com a API no ar (ou usando o `iniciar.cmd`), abra **http://localhost:8080** no navegador. A tela é servida pela própria API, não precisa instalar nada.

**Todas as funções da tela:**

| Área | O que faz |
|---|---|
| **Indicador "online" (canto superior)** | Verde quando a tela está falando com a API; vermelho "offline" se a API caiu. |
| **Saldo (topo)** | Saldo atual em destaque (fica vermelho se negativo), mais quanto **entrou** e quanto **saiu**. Atualiza sozinho após cada ação. |
| **Chat com o assistente** | Escreva em português: "gastei 80 reais no mercado, categoria alimentação", "recebi 3000 de salário", "qual é o meu saldo?", "defina um orçamento de 400 para lazer", "mostre minhas últimas transações". A IA executa a ação de verdade e responde. |
| **Atalhos do chat** | Quatro frases prontas abaixo do chat: um clique envia o exemplo. |
| **Memória da conversa** | A IA lembra do que foi dito antes na mesma conversa ("e como está o orçamento *dessa* categoria?"). |
| **Nova conversa** | Botão no topo do chat: limpa a conversa e começa uma nova memória. |
| **Microfone** | Clique, fale ("gastei quarenta e cinco reais no almoço") e clique de novo para enviar. O áudio é transcrito, aparece como "Áudio transcrito: ..." e a IA registra o gasto. O navegador pede permissão do microfone na primeira vez. |
| **Aba Transações** | Formulário manual (descrição, valor, despesa ou receita, categoria, data opcional) e lista das transações, mais recentes primeiro. Passe o mouse sobre uma linha e clique no **×** para remover. Se a despesa deixar um orçamento perto do limite, aparece um aviso. |
| **Aba Orçamentos** | Defina o limite mensal por categoria. Cada orçamento mostra quanto já foi gasto no mês e uma barra: **verde** (abaixo de 80%), **amarela** (80% ou mais) e **vermelha** (estourou). |
| **Aba Histórico de ações** | A auditoria: o que foi feito por você (API), pela IA (IA) e por áudio, com data e hora. |
| **Modo escuro** | Automático, segue o tema do seu sistema. |
| **Celular** | A tela se adapta a telas pequenas. |

Os dados ficam salvos em `./data` e continuam ali depois de fechar e abrir a API. Só o histórico da *conversa* com a IA (a memória do chat) é apagado quando a API reinicia.

**Atalho no Windows:** dê dois cliques em `iniciar.cmd`. Na primeira vez ele pede a chave do Groq e a guarda em `chave.txt` (só neste computador; o arquivo não vai para o Git). Depois sobe a API e abre a tela sozinho. Para encerrar, feche a janela do terminal.

> **Importante:** esta versão é para uso **pessoal, no seu computador**. Ela não tem login. Não a coloque na internet como está, porque qualquer pessoa com o endereço veria e alteraria seus dados e gastaria o limite da sua chave de IA.

---

## 7. Como testar (requisições)

Há duas formas. Use a que preferir — os resultados são os mesmos.

### 7.1 Pelo VS Code (`api.http`)

1. Instale a extensão **REST Client** (autor: Huachao Mao).
2. Abra o arquivo `api.http`.
3. Clique em **Send Request**, que aparece acima de cada bloco `###`.

### 7.2 Pelo terminal (`curl`) — em outro terminal, com a API rodando

Siga esta sequência. Em cada passo está o que você deve ver (os valores são exemplos).

**Passo 1 — Definir orçamento de R$ 500 para "alimentação"**
```bash
curl -X POST http://localhost:8080/api/budgets \
  -H "Content-Type: application/json" \
  -d '{"category":"alimentação","monthlyLimit":500}'
```
```json
{"category":"alimentação","level":"OK","limit":500.00,"spent":0,"remaining":500.00,
 "message":"Dentro do orçamento de 'alimentação' (0% usado)."}
```

**Passo 2 — Criar uma despesa (sem IA)**
```bash
curl -X POST http://localhost:8080/api/transactions \
  -H "Content-Type: application/json" \
  -d '{"description":"Mercado","amount":120.50,"type":"EXPENSE","category":"alimentação"}'
```
A resposta traz a transação criada **e** o campo `budget` com a situação do orçamento (24% usado, `OK`).
Para criar uma **receita**, use `"type":"INCOME"`. A data é opcional (padrão: hoje); para informar, use `"date":"2026-10-06"`.

**Passo 3 — Listar transações e ver o saldo**
```bash
curl http://localhost:8080/api/transactions
curl http://localhost:8080/api/transactions/balance
```
O saldo retorna `{"income":..., "expense":..., "balance":...}`.

**Passo 4 — Conversar com a IA (Tool Calling)**
```bash
curl -X POST http://localhost:8080/api/assistant/chat \
  -H "Content-Type: application/json" \
  -d '{"conversationId":"lucas","message":"Gastei 380 reais com jantar fora hoje, categoria alimentação"}'
```
Resposta esperada: um texto em português confirmando o registro e, como 120,50 + 380 passa de R$ 500, **avisando que o orçamento estourou**.
Confirme que a IA gravou de verdade: `curl http://localhost:8080/api/transactions` deve mostrar a nova transação.

**Passo 5 — Testar a memória da conversa** (mesmo `conversationId`)
```bash
curl -X POST http://localhost:8080/api/assistant/chat \
  -H "Content-Type: application/json" \
  -d '{"conversationId":"lucas","message":"E como está meu orçamento dessa categoria?"}'
```
Ela responde sobre "alimentação" sem você repetir, porque lembra da conversa.

**Passo 6 — Pedir para a IA definir orçamento e consultar saldo**
```bash
curl -X POST http://localhost:8080/api/assistant/chat \
  -H "Content-Type: application/json" \
  -d '{"conversationId":"lucas","message":"Defina um orçamento de 300 reais para lazer e me diga meu saldo atual"}'
```

**Passo 7 — Status do orçamento e auditoria**
```bash
curl "http://localhost:8080/api/budgets/alimenta%C3%A7%C3%A3o/status"
curl http://localhost:8080/api/budgets
curl http://localhost:8080/api/audit
```
A auditoria mostra cada ação com a origem: `API` (você), `AI_TOOL` (a IA via tool) ou `AUDIO`.

**Passo 8 — ChatModel direto (sem memória e sem tools)**
```bash
curl "http://localhost:8080/api/assistant/playground?question=O%20que%20%C3%A9%20reserva%20de%20emerg%C3%AAncia?&temperature=0.3"
```

**Passo 9 — Voz (só com OpenAI)**
```bash
# Texto -> mp3
curl -X POST http://localhost:8080/api/assistant/speech \
  -H "Content-Type: application/json" \
  -d '{"text":"Olá! Sua despesa foi registrada com sucesso."}' --output resposta.mp3

# Chat com resposta em áudio
curl -X POST http://localhost:8080/api/assistant/chat/voice \
  -H "Content-Type: application/json" \
  -d '{"conversationId":"lucas","message":"Qual é o meu saldo?"}' --output saldo.mp3
```

**Passo 10 — Áudio → transcrição → transação (OpenAI ou Groq)**

Grave um áudio curto dizendo, por exemplo, "gastei quarenta e cinco reais no almoço", salve como `gasto.mp3` (também aceita wav, m4a, webm) e rode na pasta onde ele está:
```bash
curl -X POST http://localhost:8080/api/transactions/audio \
  -F "file=@gasto.mp3" -F "conversationId=lucas"
```
Resposta:
```json
{"conversationId":"lucas","transcription":"Gastei quarenta e cinco reais no almoço.","reply":"Registrei..."}
```

> **Dica sobre categorias:** o orçamento só é conferido quando a categoria da despesa é igual à do orçamento (ignorando maiúsculas/minúsculas, mas **acentos contam**: "alimentacao" ≠ "alimentação"). Quando falar com a IA, cite a categoria, ex.: "...categoria alimentação".

### 7.3 Ver o banco de dados

Abra http://localhost:8080/h2-console e entre com:

| Campo | Valor |
|---|---|
| JDBC URL | `jdbc:h2:file:./data/smartfinance` |
| User | `sa` |
| Password | *(vazio)* |

Tabelas: `transactions`, `budgets`, `audit_logs`. Para zerar tudo, pare a API e apague a pasta `data/`.

---

## 8. Endpoints

| Método | Rota | Descrição | Precisa de IA? |
|---|---|---|---|
| GET | `/` | a **tela web** (arquivos em `src/main/resources/static`) | não |
| POST | `/api/transactions` | cria transação (retorna status do orçamento se for despesa) | não |
| GET | `/api/transactions?page=0&size=20` | lista paginada, mais recentes primeiro | não |
| GET | `/api/transactions/{id}` | busca uma transação | não |
| DELETE | `/api/transactions/{id}` | remove | não |
| GET | `/api/transactions/balance` | receitas, despesas e saldo | não |
| POST | `/api/transactions/audio` | multipart `file` (+ `conversationId`): áudio → texto → assistente registra | sim |
| POST | `/api/budgets` | define/atualiza orçamento mensal | não |
| GET | `/api/budgets` | lista orçamentos com situação atual | não |
| GET | `/api/budgets/{categoria}/status` | situação do orçamento de uma categoria | não |
| POST | `/api/assistant/chat` | chat com memória e tools | sim |
| POST | `/api/assistant/chat/voice` | chat com resposta em mp3 | sim (+ voz) |
| POST | `/api/assistant/speech` | texto → mp3 | sim (voz) |
| GET | `/api/assistant/playground` | ChatModel direto, sem memória/tools | sim |
| GET | `/api/audit?page=0&size=50` | trilha de auditoria | não |

**Tools que a IA pode chamar** (`ai/FinanceTools.java`): `registerTransaction`, `listRecentTransactions`, `getBalance`, `checkBudget`, `setBudget`.

**Níveis do orçamento:** `NO_BUDGET` (sem limite definido) · `OK` (< 80%) · `WARNING` (≥ 80%) · `EXCEEDED` (≥ 100%).

---

## 9. Testes automáticos

```bash
./run.cmd test          # ou: mvn test
```
Roda o `BudgetServiceTest`, que verifica as regras de orçamento (sem orçamento, OK, alerta de 80% e estouro). Não precisa de chave nem de internet para a IA.

---

## 10. Problemas comuns

| Sintoma | Causa / solução |
|---|---|
| `mvn: command not found` | Maven não instalado. Use `./run.cmd ...` no lugar de `mvn`. |
| `[ERRO] Java nao encontrado` | Instale o JDK 21 (seção 5) e abra um terminal novo. |
| `localhost:8080` mostra página de erro em vez da tela | Você está rodando uma versão antiga do projeto, sem a pasta `static`. Baixe o zip mais recente e rode de novo. |
| `iniciar.cmd` abre e fecha rápido, ou a tela não abre | Rode `iniciar.cmd` pelo terminal para ler a mensagem. Causas comuns: JDK 21 não instalado ou a API ainda está subindo (abra **http://localhost:8080** manualmente após 20–30 s). |
| Tela com indicador "offline" | A API não está rodando ou caiu. Veja o terminal e suba de novo. |
| Microfone não grava | Permita o microfone no navegador (cadeado ao lado do endereço). Só funciona em `localhost` ou `https`. |
| Quero trocar a chave do Groq | Apague o arquivo `chave.txt` e rode `iniciar.cmd` de novo; ele pergunta a chave nova. |
| `401` / `Incorrect API key` | Chave errada, vazia ou sem crédito. Confirme se exportou a variável **no mesmo terminal** em que rodou a API. |
| `429` (Too Many Requests) | Limite do plano gratuito atingido. Aguarde alguns minutos. |
| `Port 8080 was already in use` | Outro programa usa a porta. Feche-o ou rode com `-Dspring-boot.run.arguments=--server.port=8081`. |
| A IA "inventa" que registrou, mas nada aparece em `/api/transactions` | Alguns modelos pequenos erram o Tool Calling. Reformule a frase citando valor e categoria; confira em `/api/audit` se houve `AI_TOOL`. |
| Voz (`/speech`) falha no Groq | Esperado: o Groq não oferece essa API pelo endpoint compatível. Use OpenAI para voz. |

---

## 11. Opcional: PostgreSQL em vez de H2

```bash
docker compose up -d
./run.cmd spring-boot:run -Dspring-boot.run.profiles=groq,postgres
```
Variáveis aceitas: `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD` (padrões em `application-postgres.yml`).

---

## 12. Próximos passos

Veja o [`ROADMAP.md`](ROADMAP.md): segurança com JWT, migrations com Flyway, memória de chat persistente, streaming, RAG e observabilidade.