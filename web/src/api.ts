import type { BoardGame, GameNight, GameNightDetail, MusicRoom, MusicView, QueueEntry } from "./types";

const TOKEN_KEY = "mesa-redonda.token";
const NAME_KEY = "mesa-redonda.user";

const SEED_NAMES = ["Lucas", "Ana", "Bruno", "Carla"] as const;

export class NetworkError extends Error {
  constructor() {
    super("sobe o backend com ./gradlew bootRun na main");
    this.name = "NetworkError";
  }
}

export class ApiError extends Error {
  readonly status: number;

  constructor(message: string, status: number) {
    super(message);
    this.name = "ApiError";
    this.status = status;
  }
}

export function readToken(): string | null {
  return localStorage.getItem(TOKEN_KEY);
}

export function readUserName(): string | null {
  return localStorage.getItem(NAME_KEY);
}

export function saveSession(token: string, name: string): void {
  localStorage.setItem(TOKEN_KEY, token);
  localStorage.setItem(NAME_KEY, name);
}

export function clearSession(): void {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(NAME_KEY);
}

type SendOptions = {
  method?: string;
  body?: unknown;
  auth?: boolean;
};

export async function send(path: string, options: SendOptions = {}): Promise<{ status: number; text: string }> {
  const headers = new Headers();
  if (options.body !== undefined) {
    headers.set("Content-Type", "application/json");
  }
  if (options.auth) {
    const token = readToken();
    if (!token) {
      throw new Error("Sem token guardado. Entra no canto com o admin da aula.");
    }
    headers.set("Authorization", `Bearer ${token}`);
  }

  let response: Response;
  try {
    response = await fetch(path, {
      method: options.method ?? "GET",
      headers,
      body: options.body !== undefined ? JSON.stringify(options.body) : undefined,
      cache: "no-store",
    });
  } catch {
    throw new NetworkError();
  }

  const text = await response.text();
  if (isProxyDown(response.status, text)) {
    throw new NetworkError();
  }
  return { status: response.status, text };
}

export function serverMessage(text: string): string {
  try {
    const data = JSON.parse(text) as { message?: string };
    if (typeof data.message === "string" && data.message.trim()) {
      return data.message.trim();
    }
  } catch {
    // corpo vazio ou texto puro
  }
  return text.trim().slice(0, 180);
}

function messageFrom(status: number, text: string): string {
  const message = serverMessage(text);
  return message ? `${status} — ${message}` : `HTTP ${status}`;
}

function parseJson<T>(text: string, label: string): T {
  try {
    return JSON.parse(text) as T;
  } catch {
    throw new Error(`${label} não veio em JSON.`);
  }
}

async function getJson<T>(path: string, label: string): Promise<T> {
  const { status, text } = await send(path);
  if (status < 200 || status >= 300) {
    throw new ApiError(messageFrom(status, text), status);
  }
  return parseJson<T>(text, label);
}

export function listNights(): Promise<GameNight[]> {
  return getJson<GameNight[]>("/api/game-nights", "A lista de noites");
}

export function getNight(id: number): Promise<GameNightDetail> {
  return getJson<GameNightDetail>(`/api/game-nights/${id}`, "A noite");
}

export async function login(email: string, password: string): Promise<{ token: string; name: string }> {
  const { status, text } = await send("/api/users/login", {
    method: "POST",
    body: { email, password },
  });
  if (status !== 200) {
    throw new ApiError(messageFrom(status, text), status);
  }
  const data = parseJson<{ token?: string; user?: { name?: string } }>(text, "O login");
  if (!data.token) {
    throw new Error("O login não devolveu token.");
  }
  return { token: data.token, name: data.user?.name || email };
}

export async function createNight(): Promise<GameNight> {
  const { status, text } = await send("/api/game-nights", {
    method: "POST",
    body: {
      title: "Noite da SI",
      scheduledAt: "2026-10-29T22:00:00Z",
      location: "PUCPR",
    },
  });
  if (status !== 201) {
    throw new ApiError(messageFrom(status, text), status);
  }
  return parseJson<GameNight>(text, "A noite criada");
}

export async function placeCatan(nightId: number): Promise<{ status: number; created: boolean }> {
  const games = await getJson<BoardGame[]>("/api/board-games", "O catálogo");
  let game = games.find((item) => sameName(item.name, "Catan"));
  let created = false;
  if (!game) {
    if (games.length > 0) {
      throw new Error("O catálogo não está vazio e não tem Catan. Este passo só cria o jogo quando a lista vem vazia.");
    }
    const createdResponse = await send("/api/board-games", {
      method: "POST",
      body: {
        name: "Catan",
        minPlayers: 3,
        maxPlayers: 4,
        durationMinutes: 90,
        weight: 3,
        description: "Colonos de Catan na noite da SI.",
      },
    });
    if (createdResponse.status !== 201) {
      throw new ApiError(messageFrom(createdResponse.status, createdResponse.text), createdResponse.status);
    }
    game = parseJson<BoardGame>(createdResponse.text, "O Catan");
    created = true;
  }

  const added = await send(`/api/game-nights/${nightId}/games/${game.id}`, { method: "POST" });
  if (added.status !== 200 && added.status !== 204) {
    throw new ApiError(messageFrom(added.status, added.text), added.status);
  }
  return { status: added.status, created };
}

/**
 * Não existe GET de membros soltos. Os ids aparecem em GET da noite
 * depois que alguém já entrou. Se nenhuma noite tiver Lucas, Ana, Bruno
 * ou Carla, tenta os ids 1–40 até o POST achar um membro do Bootstrapper.
 */
export async function enrollSeed(
  nightId: number,
  current: GameNightDetail,
): Promise<{ status: number; name: string }> {
  const known = new Map<string, number>();
  collectSeeds(current, known);
  let pick = preferSeed(known);
  if (!pick) {
    const nights = await listNights();
    for (const night of nights) {
      if (night.id === nightId) continue;
      collectSeeds(await getNight(night.id), known);
      if (known.has("Lucas")) break;
    }
    pick = preferSeed(known);
  }
  if (pick) {
    const status = await postMember(nightId, pick.id);
    return { status, name: pick.name };
  }

  for (let memberId = 1; memberId <= 40; memberId += 1) {
    const { status, text } = await send(`/api/game-nights/${nightId}/members/${memberId}`, {
      method: "POST",
      body: { role: "PLAYER" },
    });
    if (status === 404 && serverMessage(text).includes("Member not found")) {
      continue;
    }
    if (status !== 200 && status !== 204) {
      throw new ApiError(messageFrom(status, text), status);
    }
    const detail = await getNight(nightId);
    const joined = detail.members.find((member) => member.memberId === memberId);
    return { status, name: joined?.displayName ?? `membro ${memberId}` };
  }

  throw new Error("Não achei Lucas, Ana, Bruno nem Carla. Não existe GET de membros soltos.");
}

export async function confirmDraft(nightId: number): Promise<GameNight> {
  const { status, text } = await send(`/api/game-nights/${nightId}/draft/confirm`, {
    method: "POST",
    auth: true,
  });
  if (status !== 200) {
    throw new ApiError(messageFrom(status, text), status);
  }
  return parseJson<GameNight>(text, "A confirmação");
}

export async function erase(
  detail: GameNightDetail,
  withoutToken: boolean,
): Promise<{ status: number; text: string; target: string }> {
  const game = detail.games.find((item) => sameName(item.name, "Catan")) ?? detail.games[0];
  const path = game
    ? `/api/game-nights/${detail.id}/games/${game.gameId}`
    : `/api/game-nights/${detail.id}`;
  const { status, text } = await send(path, {
    method: "DELETE",
    auth: !withoutToken,
  });
  return { status, text, target: `DELETE ${path}` };
}

export async function loadMusic(nightId: number | null): Promise<MusicView> {
  const probeId = nightId ?? 0;
  const roomResponse = await send(`/api/game-nights/${probeId}/music-room`);
  if (isMissingRoute(roomResponse.status, roomResponse.text)) {
    return { available: false };
  }
  if (nightId == null) {
    return {
      available: true,
      room: null,
      queue: [],
      note: "Sem noite selecionada, a fila não tem o que mostrar.",
    };
  }
  if (roomResponse.status === 404) {
    return {
      available: true,
      room: null,
      queue: [],
      note: "Sala ainda não aberta. Nada na fila.",
    };
  }
  if (roomResponse.status < 200 || roomResponse.status >= 300) {
    return {
      available: true,
      room: null,
      queue: [],
      note: messageFrom(roomResponse.status, roomResponse.text),
    };
  }

  const room = parseJson<MusicRoom>(roomResponse.text, "A sala");
  const queueResponse = await send(`/api/game-nights/${nightId}/queue`);
  if (queueResponse.status < 200 || queueResponse.status >= 300) {
    return {
      available: true,
      room,
      queue: [],
      note: messageFrom(queueResponse.status, queueResponse.text),
    };
  }
  const parsed = parseJson<unknown>(queueResponse.text, "A fila");
  if (!Array.isArray(parsed)) {
    return { available: true, room, queue: [], note: "A fila não veio como lista." };
  }
  const queue = (parsed as QueueEntry[]).slice().sort((a, b) => a.position - b.position);
  return { available: true, room, queue, note: null };
}

async function postMember(nightId: number, memberId: number): Promise<number> {
  const { status, text } = await send(`/api/game-nights/${nightId}/members/${memberId}`, {
    method: "POST",
    body: { role: "PLAYER" },
  });
  if (status !== 200 && status !== 204) {
    throw new ApiError(messageFrom(status, text), status);
  }
  return status;
}

function collectSeeds(detail: GameNightDetail, known: Map<string, number>): void {
  for (const member of detail.members) {
    if (isSeed(member.displayName) && !known.has(member.displayName)) {
      known.set(member.displayName, member.memberId);
    }
  }
}

function preferSeed(known: Map<string, number>): { name: string; id: number } | null {
  for (const name of SEED_NAMES) {
    const id = known.get(name);
    if (id != null) return { name, id };
  }
  return null;
}

function isSeed(name: string): boolean {
  return (SEED_NAMES as readonly string[]).includes(name);
}

function sameName(left: string, right: string): boolean {
  return left.localeCompare(right, "pt-BR", { sensitivity: "accent" }) === 0;
}

function isProxyDown(status: number, text: string): boolean {
  if (status < 500) return false;
  return /ECONNREFUSED|ECONNRESET|ENOTFOUND|http proxy error|proxy error|socket hang up/i.test(text);
}

function isMissingRoute(status: number, text: string): boolean {
  if (status !== 404) return false;
  try {
    const data = JSON.parse(text) as { exception?: string; message?: string };
    const exception = data.exception ?? "";
    if (exception.includes("br.pucpr.authserver.exception.NotFoundException")) return false;
    const message = data.message ?? "";
    if (message.includes("Not found. id=")) return false;
    if (message.toLowerCase().includes("music room not found")) return false;
  } catch {
    return true;
  }
  return true;
}
