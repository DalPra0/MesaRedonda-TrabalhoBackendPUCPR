export type GameNightStatus = "DRAFT" | "OPEN" | "LIVE" | "SCORED" | "CANCELLED";

export type NightRole = "HOST" | "SNACK" | "RULES" | "DJ" | "PLAYER";

export type QueueStatus = "QUEUED" | "PLAYING" | "DONE" | "SKIPPED" | "REMOVED";

export type GameNight = {
  id: number;
  title: string;
  scheduledAt: string;
  location: string;
  status: GameNightStatus;
};

export type NightGame = {
  id: number;
  gameId: number;
  name: string;
  draftOrder: number;
  confirmed: boolean;
};

export type NightMember = {
  id: number;
  memberId: number;
  displayName: string;
  email: string | null;
  seasonPoints: number;
  role: NightRole;
};

export type GameNightDetail = GameNight & {
  games: NightGame[];
  members: NightMember[];
};

export type BoardGame = {
  id: number;
  name: string;
  minPlayers: number;
  maxPlayers: number;
  durationMinutes: number;
  weight: number;
  description: string;
};

export type MusicRoom = {
  id: number;
  nightId: number;
  inviteCode: string;
  mood: string | null;
  active: boolean;
};

export type QueueEntry = {
  id: number;
  nightId: number;
  musicRoomId: number;
  trackId: number;
  title: string;
  artist: string;
  durationSeconds: number;
  memberId: number;
  displayName: string;
  position: number;
  status: QueueStatus;
  upVotes: number;
  downVotes: number;
  skipRequests: number;
};

export type MusicView =
  | { available: false }
  | {
      available: true;
      room: MusicRoom | null;
      queue: QueueEntry[];
      note: string | null;
    };
