"use client";

import { useRoomStore } from "@/store/zustand/useRoomStore";
import { sendRoom } from "../lib/roomSocket";
import { REACTIONS } from "../types";

/** Hàng nút thả cảm xúc dưới trình phát. */
export function ReactionBar() {
  return (
    <div className="flex items-center gap-1" role="group" aria-label="Thả cảm xúc">
      {REACTIONS.map((r) => (
        <button
          key={r.key}
          type="button"
          onClick={() => sendRoom("reaction", { emoji: r.key })}
          aria-label={`Thả ${r.emoji}`}
          className="flex size-9 items-center justify-center rounded-full text-xl transition hover:scale-125 hover:bg-gray-100"
        >
          {r.emoji}
        </button>
      ))}
    </div>
  );
}

const EMOJI_BY_KEY: Record<string, string> = Object.fromEntries(REACTIONS.map((r) => [r.key, r.emoji]));

/** Biểu tượng bay lên trên trình phát của mọi người; không lưu lại. */
export function ReactionOverlay() {
  const reactions = useRoomStore((s) => s.reactions);
  const dropReaction = useRoomStore((s) => s.dropReaction);

  return (
    <div className="pointer-events-none absolute inset-0 z-30 overflow-hidden" aria-hidden>
      {reactions.map((r) => (
        <span
          key={r.id}
          onAnimationEnd={() => dropReaction(r.id)}
          style={{ left: `${8 + ((r.id * 37) % 78)}%` }}
          className="room-reaction absolute bottom-14 flex flex-col items-center"
        >
          <span className="text-4xl">{EMOJI_BY_KEY[r.emoji] ?? "👍"}</span>
          {r.name && <span className="rounded bg-black/60 px-1.5 text-xs text-white">{r.name}</span>}
        </span>
      ))}
    </div>
  );
}
