import type { PlaybackState } from "@/lib/roomSync";

/** Video trong phòng (khớp dto/room/VideoRoom của backend). */
export interface RoomVideo {
  id: string;
  title: string;
  duration: number;
  thumbnailUrl?: string | null;
  videoUrl?: string | null;
  author?: string | null;
  authorImg?: string | null;
  totalView: number;
  createdAt?: string | null;
}

export interface RoomPlayback extends PlaybackState {
  lastActor?: string | null;
}

export interface RoomMember {
  userId: string;
  name: string;
  avatar?: string | null;
  host: boolean;
}

export interface RoomChatMessage {
  id: string;
  /** CHAT: người dùng nhắn; SYSTEM: thông báo của phòng. */
  type: "CHAT" | "SYSTEM";
  userId?: string | null;
  name?: string | null;
  avatar?: string | null;
  content: string;
  at: number;
}

/** Tin server gửi (dto/room/RoomEvent của backend); chỉ các trường của từng type mới có mặt. */
export interface RoomEvent {
  type: "SNAPSHOT" | "STATE" | "MEMBERS" | "QUEUE" | "CHAT" | "REACTION" | "TIME" | "CLOSED" | "KICKED" | "ERROR";
  code?: string;
  state?: RoomPlayback;
  video?: RoomVideo | null;
  members?: RoomMember[];
  hostUserId?: string;
  queue?: RoomVideo[];
  chat?: RoomChatMessage[];
  message?: RoomChatMessage;
  emoji?: string;
  userId?: string;
  name?: string;
  serverTime?: number;
  clientSent?: number;
  /** NOT_FOUND, KICKED, FULL (không vào được phòng) hoặc RATE, INVALID (báo lỗi nhỏ). */
  error?: string;
  text?: string;
}

export type RoomAction = "leave" | "control" | "chat" | "reaction" | "queue" | "kick" | "close" | "time";

export type RoomControlType = "PLAY" | "PAUSE" | "SEEK" | "CHANGE" | "ENDED";

export const REACTIONS = [
  { key: "like", emoji: "👍" },
  { key: "love", emoji: "❤️" },
  { key: "laugh", emoji: "😂" },
  { key: "wow", emoji: "😮" },
  { key: "sad", emoji: "😢" },
  { key: "fire", emoji: "🔥" },
] as const;
