import { create } from "zustand";
import type { RoomChatMessage, RoomEvent, RoomMember, RoomPlayback, RoomVideo } from "@/modules/rooms/types";

/** Trạng thái phòng xem chung đang mở, cập nhật từ các tin server gửi (modules/rooms/lib/roomSocket). */
export type RoomStatus = "idle" | "connecting" | "joined" | "closed" | "kicked" | "error";

const MAX_CHAT = 200;

interface RoomState {
  code: string | null;
  status: RoomStatus;
  /** NOT_FOUND, KICKED, FULL khi không vào được phòng. */
  errorCode: string | null;
  errorText: string | null;
  playback: RoomPlayback | null;
  video: RoomVideo | null;
  members: RoomMember[];
  hostUserId: string | null;
  queue: RoomVideo[];
  chat: RoomChatMessage[];
  /** Giờ server ≈ giờ máy + clockOffset (ms). */
  clockOffset: number;
  reactions: { id: number; emoji: string; name: string }[];
  /** ok; muted: trình duyệt chặn phát có tiếng nên đang phát không tiếng; blocked: chặn cả phát không tiếng. */
  autoplay: "ok" | "muted" | "blocked";

  open: (code: string) => void;
  close: () => void;
  setOffset: (offset: number) => void;
  apply: (event: RoomEvent) => void;
  dropReaction: (id: number) => void;
  setAutoplay: (autoplay: "ok" | "muted" | "blocked") => void;
}

const empty = {
  code: null,
  status: "idle" as RoomStatus,
  errorCode: null,
  errorText: null,
  playback: null,
  video: null,
  members: [],
  hostUserId: null,
  queue: [],
  chat: [],
  clockOffset: 0,
  reactions: [],
  autoplay: "ok" as const,
};

let reactionId = 0;

export const useRoomStore = create<RoomState>((set) => ({
  ...empty,

  open: (code) => set({ ...empty, code, status: "connecting" }),
  close: () => set({ ...empty }),
  setOffset: (offset) => set({ clockOffset: offset }),
  setAutoplay: (autoplay) => set({ autoplay }),
  dropReaction: (id) => set((s) => ({ reactions: s.reactions.filter((r) => r.id !== id) })),

  apply: (event) =>
    set((s) => {
      switch (event.type) {
        case "SNAPSHOT":
          return {
            status: "joined",
            playback: event.state ?? null,
            video: event.video ?? null,
            members: event.members ?? [],
            hostUserId: event.hostUserId ?? null,
            queue: event.queue ?? [],
            chat: event.chat ?? [],
            // Lần đo đầu tiên (thô, chưa trừ độ trễ mạng); các lần đo chính xác hơn tới sau qua setOffset
            clockOffset: event.serverTime ? event.serverTime - Date.now() : s.clockOffset,
          };
        case "STATE":
          // Tin cũ hơn trạng thái đang có thì bỏ (hai tin tới không đúng thứ tự)
          if (s.playback && event.state && event.state.version < s.playback.version) return {};
          return { playback: event.state ?? s.playback, video: event.video ?? null };
        case "MEMBERS":
          return { members: event.members ?? [], hostUserId: event.hostUserId ?? s.hostUserId };
        case "QUEUE":
          return { queue: event.queue ?? [] };
        case "CHAT": {
          const message = event.message;
          if (!message || s.chat.some((c) => c.id === message.id)) return {};
          return { chat: [...s.chat, message].slice(-MAX_CHAT) };
        }
        case "REACTION":
          if (!event.emoji) return {};
          return {
            reactions: [...s.reactions.slice(-30), { id: ++reactionId, emoji: event.emoji, name: event.name ?? "" }],
          };
        case "CLOSED":
          return { status: "closed" };
        case "KICKED":
          return { status: "kicked" };
        case "ERROR":
          // Không vào được phòng: dừng ở màn hình lỗi. Lỗi nhỏ (RATE, INVALID) do nơi gọi hiện toast
          if (event.error === "NOT_FOUND" || event.error === "KICKED" || event.error === "FULL") {
            return { status: "error", errorCode: event.error, errorText: event.text ?? null };
          }
          return {};
        default:
          return {};
      }
    }),
}));
