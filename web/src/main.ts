import "./style.css";
import {
  clearSession,
  confirmDraft,
  createNight,
  enrollSeed,
  erase,
  getNight,
  listNights,
  loadMusic,
  login,
  NetworkError,
  placeCatan,
  readToken,
  readUserName,
  saveSession,
  serverMessage,
} from "./api";
import type { GameNightDetail, MusicView, NightGame, NightMember, NightRole, QueueEntry } from "./types";

type ResultKind = "ok" | "erro" | "esperado";

const state: {
  busy: boolean;
  loading: boolean;
  seenData: boolean;
  nights: Awaited<ReturnType<typeof listNights>>;
  detail: GameNightDetail | null;
  selectedId: number | null;
  music: MusicView | null;
  banner: string | null;
  updatedAt: Date | null;
} = {
  busy: false,
  loading: true,
  seenData: false,
  nights: [],
  detail: null,
  selectedId: null,
  music: null,
  banner: null,
  updatedAt: null,
};

let refreshing = false;

const STATUS: Record<string, string> = {
  DRAFT: "rascunho",
  OPEN: "aberta",
  LIVE: "ao vivo",
  SCORED: "pontuada",
  CANCELLED: "cancelada",
};

const ROLES: Record<NightRole, string> = {
  HOST: "anfitrião",
  SNACK: "lanche",
  RULES: "regras",
  DJ: "DJ",
  PLAYER: "jogador",
};

const QUEUE: Record<string, string> = {
  QUEUED: "na fila",
  PLAYING: "tocando",
  DONE: "já tocou",
  SKIPPED: "pulada",
  REMOVED: "tirada",
};

function must<T extends Element>(selector: string): T {
  const node = document.querySelector(selector);
  if (!node) throw new Error(`Falta ${selector} na página.`);
  return node as T;
}

function el<K extends keyof HTMLElementTagNameMap>(
  tag: K,
  className?: string,
  text?: string,
): HTMLElementTagNameMap[K] {
  const node = document.createElement(tag);
  if (className) node.className = className;
  if (text !== undefined) node.textContent = text;
  return node;
}

function errorText(error: unknown): string {
  if (error instanceof Error) return error.message;
  return "Falha inesperada.";
}

function currentStep(night: GameNightDetail | null): number {
  if (!night) return 1;
  if (night.status !== "DRAFT") return 5;
  if (night.games.length === 0) return 2;
  if (night.members.length === 0) return 3;
  return 4;
}

function formatWhen(iso: string): string {
  const date = new Date(iso);
  if (Number.isNaN(date.getTime())) return iso;
  return new Intl.DateTimeFormat("pt-BR", {
    weekday: "short",
    day: "2-digit",
    month: "short",
    hour: "2-digit",
    minute: "2-digit",
  }).format(date);
}

function formatClock(date: Date): string {
  return new Intl.DateTimeFormat("pt-BR", {
    hour: "2-digit",
    minute: "2-digit",
    second: "2-digit",
  }).format(date);
}

function initials(name: string): string {
  const parts = name.trim().split(/\s+/).filter(Boolean);
  if (parts.length === 0) return "?";
  return parts
    .slice(0, 2)
    .map((part) => part[0] ?? "")
    .join("")
    .toLocaleUpperCase("pt-BR");
}

function gameOnTable(detail: GameNightDetail): NightGame | undefined {
  return detail.games.find((game) => game.name.localeCompare("Catan", "pt-BR", { sensitivity: "accent" }) === 0)
    ?? detail.games[0];
}

function setResult(text: string, kind: ResultKind): void {
  const node = must<HTMLParagraphElement>("#resultado");
  node.textContent = text;
  node.className = `resultado ${kind}`;
}

function paint(): void {
  paintSession();
  paintBanner();
  paintSteps();
  paintNights();
  paintFelt();
  paintQueue();
  const clock = must<HTMLParagraphElement>("#relogio");
  clock.textContent = state.updatedAt ? `API às ${formatClock(state.updatedAt)}` : "";
}

function paintSession(): void {
  const estado = must<HTMLParagraphElement>("#login-estado");
  const sair = must<HTMLButtonElement>("#sair");
  const token = readToken();
  const name = readUserName();
  if (token) {
    estado.textContent = name ? `token de ${name}` : "token guardado";
    sair.hidden = false;
  } else {
    estado.textContent = "sem sessão";
    sair.hidden = true;
  }
  sair.disabled = state.busy;
  must<HTMLButtonElement>("#login button[type='submit']").disabled = state.busy;
  must<HTMLInputElement>("#sem-token").disabled = state.busy;
}

function paintBanner(): void {
  const aviso = must<HTMLParagraphElement>("#aviso");
  if (state.banner) {
    aviso.hidden = false;
    aviso.textContent = state.banner;
  } else {
    aviso.hidden = true;
    aviso.textContent = "";
  }
}

function paintSteps(): void {
  const step = currentStep(state.detail);
  const without = must<HTMLInputElement>("#sem-token").checked;
  for (let index = 1; index <= 5; index += 1) {
    const item = must<HTMLLIElement>(`#passo-${index}`);
    item.classList.toggle("atual", index === step);
    item.classList.toggle("feito", index < step);
    if (index === step) item.setAttribute("aria-current", "step");
    else item.removeAttribute("aria-current");
    const button = item.querySelector("button");
    if (button) {
      const needsNight = index > 1;
      button.disabled = state.busy || (needsNight && state.detail == null);
    }
  }

  const apagar = must<HTMLButtonElement>("#acao-apagar");
  apagar.textContent = without ? "Apagar sem token" : "Apagar com token";
  const endpoint = must<HTMLParagraphElement>("#endpoint-apagar");
  const detail = state.detail;
  if (!detail) {
    endpoint.textContent = "DELETE da noite ou de um jogo dela";
    return;
  }
  const game = gameOnTable(detail);
  const path = game
    ? `/api/game-nights/${detail.id}/games/${game.gameId}`
    : `/api/game-nights/${detail.id}`;
  endpoint.textContent = `DELETE ${path} · ${without ? "sem token" : "com token"}`;
}

function paintNights(): void {
  const host = must<HTMLDivElement>("#noites");
  host.replaceChildren();
  for (const night of state.nights) {
    const button = el("button", night.id === state.selectedId ? "noite escolhida" : "noite", night.title);
    button.type = "button";
    button.disabled = state.busy;
    button.addEventListener("click", () => {
      if (state.busy) return;
      state.selectedId = night.id;
      void refresh();
    });
    host.append(button);
  }
}

function paintFelt(): void {
  const felt = must<HTMLDivElement>("#feltro");
  felt.replaceChildren();
  if (state.loading && !state.seenData) {
    felt.append(el("p", "vazio", "carregando a mesa…"));
    return;
  }
  if (state.banner && !state.seenData) {
    felt.append(el("p", "vazio", state.banner));
    return;
  }
  if (!state.detail) {
    felt.append(el("p", "vazio", "nenhuma noite ainda"));
    return;
  }

  const detail = state.detail;
  const board = el("div", "mesa-miolo");
  const heading = el("div");
  heading.append(el("h3", undefined, detail.title));
  heading.append(el(
    "p",
    "meta",
    `#${detail.id} · ${formatWhen(detail.scheduledAt)} · ${detail.location} · ${STATUS[detail.status] ?? detail.status}`,
  ));
  board.append(heading);

  const cards = el("div", "cartas");
  if (detail.games.length === 0) {
    cards.append(el("p", "nota", "nenhum jogo na mesa ainda"));
  } else {
    for (const game of detail.games) cards.append(gameCard(game));
  }
  board.append(cards);

  const people = el("div", "fichas");
  if (detail.members.length === 0) {
    people.append(el("p", "nota", "ninguém confirmou ainda"));
  } else {
    for (const member of detail.members) people.append(personChip(member));
  }
  board.append(people);
  felt.append(board);
}

function gameCard(game: NightGame): HTMLElement {
  const card = el("article", "carta");
  card.append(el("span", "ordem", `#${game.draftOrder}`));
  card.append(el("h3", undefined, game.name));
  card.append(el(
    "p",
    game.confirmed ? "selo confirmado" : "selo draft",
    game.confirmed ? "confirmado" : "draft",
  ));
  return card;
}

function personChip(member: NightMember): HTMLElement {
  const chip = el("div", "ficha");
  const role = member.role.toLowerCase();
  chip.append(el("span", `disco ${role}`, initials(member.displayName)));
  const text = el("span");
  text.append(el("strong", undefined, member.displayName));
  text.append(el("small", undefined, `${ROLES[member.role] ?? member.role} · ${member.seasonPoints} pts`));
  chip.append(text);
  if (member.email) chip.title = member.email;
  return chip;
}

function paintQueue(): void {
  const section = must<HTMLElement>("#zona-fila");
  const layout = must<HTMLElement>(".zonas");
  const host = must<HTMLDivElement>("#fila");
  if (!state.music?.available) {
    section.hidden = true;
    layout.classList.remove("com-fila");
    host.replaceChildren();
    return;
  }

  section.hidden = false;
  layout.classList.add("com-fila");
  host.replaceChildren();
  const music = state.music;
  if (music.room) {
    const mood = music.room.mood?.trim() ? music.room.mood : "sem clima definido";
    const sala = el(
      "p",
      "sala",
      `Convite ${music.room.inviteCode} · ${mood} · ${music.room.active ? "ativa" : "inativa"}`,
    );
    host.append(sala);
  }
  if (music.note) host.append(el("p", "nota", music.note));
  if (music.queue.length === 0 && !music.note) {
    host.append(el("p", "nota", "fila vazia"));
  }
  for (const entry of music.queue) host.append(queueRow(entry));
}

function queueRow(entry: QueueEntry): HTMLElement {
  const row = el("article", "faixa");
  if (entry.status === "PLAYING") row.classList.add("tocando");
  if (entry.status === "REMOVED") row.classList.add("tirada");
  row.append(el("span", "pos", String(entry.position)));
  const text = el("div");
  text.append(el("strong", undefined, entry.title));
  text.append(el("span", undefined, `${entry.artist} · ${entry.displayName}`));
  row.append(text);
  const votes = entry.upVotes + entry.downVotes + entry.skipRequests;
  row.append(el("span", undefined, `${QUEUE[entry.status] ?? entry.status} · ${votes} votos`));
  return row;
}

async function refresh(): Promise<void> {
  if (state.busy || refreshing) return;
  refreshing = true;
  try {
    try {
      const nights = await listNights();
      state.nights = nights;
      state.seenData = true;
      if (state.selectedId != null && !nights.some((night) => night.id === state.selectedId)) {
        state.selectedId = null;
      }
      if (state.selectedId == null && nights[0]) {
        state.selectedId = nights[0].id;
      }
      state.detail = state.selectedId == null ? null : await getNight(state.selectedId);
      state.banner = null;
      state.updatedAt = new Date();
    } catch (error) {
      state.banner = errorText(error);
      if (error instanceof NetworkError) state.music = { available: false };
      return;
    }

    try {
      state.music = await loadMusic(state.selectedId);
    } catch (error) {
      state.banner = errorText(error);
      if (error instanceof NetworkError) state.music = { available: false };
    }
  } finally {
    state.loading = false;
    refreshing = false;
    paint();
  }
}

async function run(action: () => Promise<string>): Promise<void> {
  if (state.busy) return;
  state.busy = true;
  paint();
  try {
    setResult(await action(), "ok");
  } catch (error) {
    setResult(errorText(error), "erro");
  } finally {
    state.busy = false;
  }
  await refresh();
}

function bind(): void {
  must<HTMLFormElement>("#login").addEventListener("submit", (event) => {
    event.preventDefault();
    const form = must<HTMLFormElement>("#login");
    const data = new FormData(form);
    const email = String(data.get("email") ?? "").trim();
    const password = String(data.get("password") ?? "");
    void run(async () => {
      const session = await login(email, password);
      saveSession(session.token, session.name);
      return `Entrou como ${session.name}.`;
    });
  });

  must<HTMLButtonElement>("#sair").addEventListener("click", () => {
    clearSession();
    setResult("Saiu. O token saiu do navegador.", "ok");
    paint();
  });

  must<HTMLInputElement>("#sem-token").addEventListener("change", () => {
    paintSteps();
  });

  must<HTMLButtonElement>("#acao-criar").addEventListener("click", () => {
    void run(async () => {
      const night = await createNight();
      state.selectedId = night.id;
      return `Noite ${night.id} criada. ${night.title}.`;
    });
  });

  must<HTMLButtonElement>("#acao-jogo").addEventListener("click", () => {
    const nightId = state.detail?.id;
    if (nightId == null) return;
    void run(async () => {
      const placed = await placeCatan(nightId);
      const created = placed.created ? "Catálogo vazio: Catan foi criado. " : "";
      const joined = placed.status === 204 ? "Catan já estava na mesa." : "Catan entrou na mesa.";
      return `${created}${joined} (${placed.status})`;
    });
  });

  must<HTMLButtonElement>("#acao-membro").addEventListener("click", () => {
    const detail = state.detail;
    if (!detail) return;
    void run(async () => {
      const joined = await enrollSeed(detail.id, detail);
      const verb = joined.status === 204 ? "já estava na mesa" : "entrou na mesa";
      return `${joined.name} ${verb}. (${joined.status})`;
    });
  });

  must<HTMLButtonElement>("#acao-draft").addEventListener("click", () => {
    const nightId = state.detail?.id;
    if (nightId == null) return;
    void run(async () => {
      const night = await confirmDraft(nightId);
      return `Draft confirmado. A noite está ${STATUS[night.status] ?? night.status}.`;
    });
  });

  must<HTMLButtonElement>("#acao-apagar").addEventListener("click", () => {
    void onErase();
  });
}

async function onErase(): Promise<void> {
  if (state.busy || !state.detail) return;
  const detail = state.detail;
  const without = must<HTMLInputElement>("#sem-token").checked;
  state.busy = true;
  paint();
  try {
    const outcome = await erase(detail, without);
    const success = without
      ? outcome.status === 403
      : outcome.status === 200 || outcome.status === 204;
    const frase = without ? "Sem login o delete morre." : "Com o token, apaga.";
    if (success) {
      setResult(`${outcome.target} → ${outcome.status}. ${frase}`, without ? "esperado" : "ok");
    } else {
      const detailText = serverMessage(outcome.text);
      const extra = detailText ? ` ${detailText}` : "";
      const expected = without ? "403" : "200 ou 204";
      setResult(`${outcome.target} → ${outcome.status}. Esperava ${expected}.${extra}`, "erro");
    }
  } catch (error) {
    setResult(errorText(error), "erro");
  } finally {
    state.busy = false;
  }
  await refresh();
}

bind();
paint();
void refresh();
window.setInterval(() => {
  void refresh();
}, 3000);
