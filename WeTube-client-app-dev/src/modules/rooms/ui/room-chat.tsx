"use client";

import { Send } from "lucide-react";
import { useEffect, useRef, useState } from "react";
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar";
import LinkifiedText from "@/modules/watch/components/linkified-text";
import { useRoomStore } from "@/store/zustand/useRoomStore";
import { sendRoom } from "../lib/roomSocket";

const MAX_LENGTH = 500;

export default function RoomChat({ className }: { className?: string }) {
  const chat = useRoomStore((s) => s.chat);
  const [text, setText] = useState("");
  const listRef = useRef<HTMLDivElement>(null);
  const stickToBottom = useRef(true);

  // Đang đọc tin cũ (cuộn lên) thì không kéo xuống khi có tin mới
  useEffect(() => {
    const el = listRef.current;
    if (el && stickToBottom.current) el.scrollTop = el.scrollHeight;
  }, [chat]);

  const submit = (e: React.FormEvent) => {
    e.preventDefault();
    const content = text.trim();
    if (!content) return;
    sendRoom("chat", { content });
    setText("");
    stickToBottom.current = true;
  };

  return (
    <div className={className}>
      <h2 className="border-b px-4 py-3 text-base font-semibold">Trò chuyện</h2>
      <div
        ref={listRef}
        onScroll={(e) => {
          const el = e.currentTarget;
          stickToBottom.current = el.scrollHeight - el.scrollTop - el.clientHeight < 40;
        }}
        className="min-h-0 flex-1 space-y-2 overflow-y-auto px-4 py-3"
      >
        {chat.length === 0 && <p className="text-sm text-gray-500">Chưa có tin nhắn nào. Hãy chào mọi người!</p>}
        {chat.map((m) =>
          m.type === "SYSTEM" ? (
            <p key={m.id} className="text-center text-xs text-gray-500">
              {m.content}
            </p>
          ) : (
            <div key={m.id} className="flex items-start gap-2">
              <Avatar className="size-7">
                <AvatarImage src={m.avatar ?? undefined} alt="" />
                <AvatarFallback>{(m.name ?? "?").slice(0, 1).toUpperCase()}</AvatarFallback>
              </Avatar>
              <div className="min-w-0 text-sm">
                <span className="mr-2 font-semibold text-gray-700">{m.name}</span>
                <LinkifiedText text={m.content} className="break-words" />
              </div>
            </div>
          )
        )}
      </div>
      <form onSubmit={submit} className="flex items-center gap-2 border-t p-3">
        <input
          value={text}
          onChange={(e) => setText(e.target.value)}
          maxLength={MAX_LENGTH}
          placeholder="Nhắn tin cho cả phòng"
          aria-label="Tin nhắn"
          className="min-w-0 flex-1 rounded-full border px-4 py-2 text-sm outline-none focus:border-blue-500"
        />
        <button
          type="submit"
          disabled={!text.trim()}
          aria-label="Gửi"
          className="flex size-9 shrink-0 items-center justify-center rounded-full bg-blue-600 text-white hover:bg-blue-700 disabled:opacity-40"
        >
          <Send className="size-4" />
        </button>
      </form>
    </div>
  );
}
