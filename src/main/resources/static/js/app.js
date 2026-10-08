/* Smart Finance — front-end simples (sem framework). Fala com a API em /api/... */
const $ = (s) => document.querySelector(s);
const brl = new Intl.NumberFormat("pt-BR", { style: "currency", currency: "BRL" });
const esc = (t) => String(t ?? "").replace(/[&<>"']/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]));
const fmtDate = (iso) => (iso ? iso.split("-").reverse().slice(0, 2).join("/") : "");
const fmtDateTime = (iso) => new Date(iso).toLocaleString("pt-BR", { dateStyle: "short", timeStyle: "short" });

let conversationId = newId();

/* ---------- Sessão (login) ---------- */
const store = {
  get: (k) => { try { return localStorage.getItem(k); } catch { return null; } },
  set: (k, v) => { try { localStorage.setItem(k, v); } catch { /* sem storage */ } },
  del: (k) => { try { localStorage.removeItem(k); } catch { /* sem storage */ } },
};
let token = store.get("sf_token");
function newId() { return "web-" + Math.random().toString(36).slice(2, 8); }

/* ---------- API ---------- */
async function api(path, options = {}) {
  const headers = { ...(options.headers || {}) };
  if (token) headers.Authorization = "Bearer " + token;
  let res;
  try { res = await fetch(path, { ...options, headers }); }
  catch { throw new Error("Não consegui falar com a API. Ela está rodando?"); }
  if (res.status === 401 && !path.startsWith("/api/auth/")) {
    endSession();
    throw new Error("Sua sessão expirou. Entre novamente.");
  }
  if (!res.ok) {
    let detail = "Erro " + res.status;
    try { const body = await res.json(); detail = body.detail || body.message || detail; } catch { /* sem corpo */ }
    throw new Error(detail);
  }
  return res.status === 204 ? null : res.json();
}
const json = (method, body) => ({ method, headers: { "Content-Type": "application/json" }, body: JSON.stringify(body) });

/* ---------- UI helpers ---------- */
let toastTimer;
function toast(msg) {
  const el = $("#toast");
  el.textContent = msg; el.classList.add("show");
  clearTimeout(toastTimer); toastTimer = setTimeout(() => el.classList.remove("show"), 3800);
}
function setStatus(state, text) { $("#status").dataset.state = state; $("#statusText").textContent = text; }

/* ---------- Dados ---------- */
async function loadBalance() {
  const b = await api("/api/transactions/balance");
  $("#income").textContent = brl.format(b.income);
  $("#expense").textContent = brl.format(b.expense);
  const el = $("#balance");
  el.textContent = brl.format(b.balance);
  el.classList.toggle("neg", Number(b.balance) < 0);
}

async function loadTransactions() {
  const page = await api("/api/transactions?size=50");
  const list = $("#txList");
  if (!page.content.length) { list.innerHTML = '<li class="empty">Nenhuma transação ainda. Adicione uma acima ou diga ao assistente o que você gastou.</li>'; return; }
  list.innerHTML = page.content.map((t) => {
    const isIn = t.type === "INCOME";
    return `<li>
      <div class="tx-main"><div class="tx-title">${esc(t.description)}</div><div class="tx-sub">${esc(t.category)} · ${fmtDate(t.date)}</div></div>
      <div class="tx-amount ${isIn ? "in" : "out"}">${isIn ? "+" : "−"} ${brl.format(t.amount)}</div>
      <button class="del" data-id="${t.id}" aria-label="Remover transação" title="Remover">×</button>
    </li>`;
  }).join("");
}

async function loadBudgets() {
  const items = await api("/api/budgets");
  const list = $("#budgetList");
  if (!items.length) { list.innerHTML = '<li class="empty">Nenhum orçamento definido. Crie um acima ou peça ao assistente: "defina um orçamento de 400 para lazer".</li>'; return; }
  list.innerHTML = items.map((b) => {
    const pct = b.limit ? Math.min(100, (Number(b.spent) / Number(b.limit)) * 100) : 0;
    return `<li class="budget" data-level="${b.level}">
      <div class="budget-top"><span class="budget-name">${esc(b.category)}</span>
        <span class="budget-nums">${brl.format(b.spent ?? 0)} de ${brl.format(b.limit ?? 0)}</span></div>
      <div class="meter"><span style="width:${pct.toFixed(0)}%"></span></div>
      <div class="budget-msg">${esc(b.message)}</div>
    </li>`;
  }).join("");
}

async function loadAudit() {
  const page = await api("/api/audit?size=40");
  const list = $("#auditList");
  if (!page.content.length) { list.innerHTML = '<li class="empty">Sem registros ainda.</li>'; return; }
  list.innerHTML = page.content.map((a) => `<li>
      <div class="tx-main"><div class="tx-title">${esc(a.action)}</div><div class="tx-sub">${esc(a.details)}</div></div>
      <div style="text-align:right"><span class="audit-source ${a.source}">${a.source === "AI_TOOL" ? "IA" : a.source === "AUDIO" ? "Áudio" : "API"}</span>
      <div class="tx-sub">${fmtDateTime(a.createdAt)}</div></div></li>`).join("");
}

async function refreshAll() {
  try {
    await Promise.all([loadBalance(), loadTransactions(), loadBudgets(), loadAudit()]);
    setStatus("ok", "online");
  } catch (e) { setStatus("off", "offline"); toast(e.message); }
}

/* ---------- Chat ---------- */
const messages = $("#messages");
function addMsg(kind, text, extra = "") {
  const el = document.createElement("div");
  el.className = `msg ${kind} ${extra}`.trim();
  el.textContent = text;
  messages.appendChild(el);
  messages.scrollTop = messages.scrollHeight;
  return el;
}
function addTyping() {
  const el = document.createElement("div");
  el.className = "msg bot";
  el.innerHTML = '<span class="typing"><i></i><i></i><i></i></span>';
  messages.appendChild(el); messages.scrollTop = messages.scrollHeight;
  return el;
}
function setBusy(busy) { $("#sendBtn").disabled = busy; $("#chatInput").disabled = busy; $("#micBtn").disabled = busy && !recorder; }

function greet() {
  messages.innerHTML = "";
  addMsg("bot", "Oi! Me diga o que você gastou ou recebeu e eu registro. Também posso mostrar seu saldo, seu histórico e avisar quando um orçamento estiver perto do limite.");
}
const SUGGESTIONS = ["Gastei 50 reais no mercado, categoria alimentação", "Recebi 3000 de salário", "Defina um orçamento de 400 reais para lazer", "Qual é o meu saldo?"];
$("#chips").innerHTML = SUGGESTIONS.map((s) => `<button type="button" class="chip">${esc(s)}</button>`).join("");
$("#chips").addEventListener("click", (e) => { if (e.target.matches(".chip")) { $("#chatInput").value = e.target.textContent; $("#chatForm").requestSubmit(); } });

$("#chatForm").addEventListener("submit", async (e) => {
  e.preventDefault();
  const input = $("#chatInput");
  const text = input.value.trim();
  if (!text) return;
  input.value = "";
  addMsg("me", text);
  const typing = addTyping();
  setBusy(true);
  try {
    const res = await api("/api/assistant/chat", json("POST", { conversationId, message: text }));
    typing.remove(); addMsg("bot", res.reply);
    refreshAll();
  } catch (err) { typing.remove(); addMsg("err", err.message); }
  finally { setBusy(false); input.focus(); }
});
$("#newChat").addEventListener("click", () => { conversationId = newId(); greet(); });

/* ---------- Áudio (microfone -> /api/transactions/audio) ---------- */
let recorder = null, chunks = [];
$("#micBtn").addEventListener("click", async () => {
  if (recorder) { recorder.stop(); return; }
  if (!navigator.mediaDevices?.getUserMedia) { toast("Seu navegador não permite gravar áudio aqui."); return; }
  let stream;
  try { stream = await navigator.mediaDevices.getUserMedia({ audio: true }); }
  catch { toast("Permita o uso do microfone para gravar."); return; }

  chunks = [];
  recorder = new MediaRecorder(stream);
  recorder.ondataavailable = (ev) => ev.data.size && chunks.push(ev.data);
  recorder.onstop = async () => {
    stream.getTracks().forEach((t) => t.stop());
    $("#micBtn").classList.remove("recording");
    const blob = new Blob(chunks, { type: recorder.mimeType || "audio/webm" });
    recorder = null;
    await sendAudio(blob);
  };
  recorder.start();
  $("#micBtn").classList.add("recording");
  toast("Gravando… clique no microfone de novo para enviar.");
});

async function sendAudio(blob) {
  const form = new FormData();
  const ext = blob.type.includes("ogg") ? "ogg" : blob.type.includes("mp4") ? "m4a" : "webm";
  form.append("file", blob, `gravacao.${ext}`);
  form.append("conversationId", conversationId);
  const typing = addTyping();
  setBusy(true);
  try {
    const res = await api("/api/transactions/audio", { method: "POST", body: form });
    typing.remove();
    addMsg("me", res.transcription, "voice");
    addMsg("bot", res.reply);
    refreshAll();
  } catch (err) { typing.remove(); addMsg("err", err.message); }
  finally { setBusy(false); }
}

/* ---------- Abas e formulários ---------- */
document.querySelectorAll(".tab").forEach((tab) => tab.addEventListener("click", () => {
  document.querySelectorAll(".tab").forEach((t) => { t.classList.toggle("active", t === tab); t.setAttribute("aria-selected", t === tab); });
  document.querySelectorAll(".pane").forEach((p) => (p.hidden = p.id !== "pane-" + tab.dataset.tab));
}));

$("#txForm").addEventListener("submit", async (e) => {
  e.preventDefault();
  const f = new FormData(e.target);
  const body = { description: f.get("description"), amount: Number(f.get("amount")), type: f.get("type"), category: f.get("category"), date: f.get("date") || null };
  try {
    const t = await api("/api/transactions", json("POST", body));
    e.target.reset();
    if (t.budget && (t.budget.level === "WARNING" || t.budget.level === "EXCEEDED")) toast(t.budget.message);
    else toast("Transação adicionada.");
    refreshAll();
  } catch (err) { toast(err.message); }
});

$("#budgetForm").addEventListener("submit", async (e) => {
  e.preventDefault();
  const f = new FormData(e.target);
  try {
    await api("/api/budgets", json("POST", { category: f.get("category"), monthlyLimit: Number(f.get("monthlyLimit")) }));
    e.target.reset(); toast("Orçamento salvo."); refreshAll();
  } catch (err) { toast(err.message); }
});

$("#txList").addEventListener("click", async (e) => {
  const btn = e.target.closest(".del");
  if (!btn) return;
  if (!confirm("Remover esta transação?")) return;
  try { await api("/api/transactions/" + btn.dataset.id, { method: "DELETE" }); toast("Transação removida."); refreshAll(); }
  catch (err) { toast(err.message); }
});

/* ---------- Login, cadastro e sair ---------- */
function showAuth() {
  $("#app").hidden = true;
  $("#auth").hidden = false;
  $("#authError").hidden = true;
}
function showApp(user) {
  $("#auth").hidden = true;
  $("#app").hidden = false;
  $("#userName").textContent = user.name;
  conversationId = newId();
  greet();
  refreshAll();
}
function endSession() {
  token = null;
  store.del("sf_token");
  showAuth();
}
function authError(msg) {
  const el = $("#authError");
  el.textContent = msg;
  el.hidden = false;
}

document.querySelectorAll(".auth-tab").forEach((tab) => tab.addEventListener("click", () => {
  document.querySelectorAll(".auth-tab").forEach((t) => { t.classList.toggle("active", t === tab); t.setAttribute("aria-selected", t === tab); });
  $("#loginForm").hidden = tab.dataset.auth !== "login";
  $("#registerForm").hidden = tab.dataset.auth !== "register";
  $("#authError").hidden = true;
}));

document.querySelectorAll(".pw-toggle").forEach((btn) => btn.addEventListener("click", () => {
  const input = btn.previousElementSibling;
  const show = input.type === "password";
  input.type = show ? "text" : "password";
  btn.textContent = show ? "Ocultar" : "Mostrar";
  btn.setAttribute("aria-pressed", show);
  btn.setAttribute("aria-label", show ? "Ocultar senha" : "Mostrar senha");
}));

async function submitAuth(e, path) {
  e.preventDefault();
  const form = e.target;
  const btn = form.querySelector("button[type=submit]");
  const body = Object.fromEntries(new FormData(form));
  btn.disabled = true;
  try {
    const res = await api(path, json("POST", body));
    token = res.token;
    store.set("sf_token", token);
    form.reset();
    showApp(res.user);
  } catch (err) { authError(err.message); }
  finally { btn.disabled = false; }
}
$("#loginForm").addEventListener("submit", (e) => submitAuth(e, "/api/auth/login"));
$("#registerForm").addEventListener("submit", (e) => submitAuth(e, "/api/auth/register"));

$("#logoutBtn").addEventListener("click", async () => {
  try { await api("/api/auth/logout", { method: "POST" }); } catch { /* sai mesmo assim */ }
  endSession();
});

/* ---------- Início ---------- */
(async function start() {
  if (!token) { showAuth(); return; }
  try { showApp(await api("/api/auth/me")); }
  catch { endSession(); }
})();
