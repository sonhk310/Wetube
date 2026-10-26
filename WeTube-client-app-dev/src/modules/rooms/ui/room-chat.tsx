"use client";

import { Send } from "lucide-react";
import { useEffect, useRef, useState } from "react";
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar";
import { useAuth } from "@/contexts/auth-context";
import LinkifiedText, { mentionsHandle } from "@/modules/watch/components/linkified-text";
import { useRoomStore } from "@/store/zustand/useRoomStore";
import { sendRoom } from "../lib/roomSocket";
import type { RoomMember } from "../types";

const MAX_LENGTH = 500;

export default function RoomChat({ className }: { className?: string }) {
  const chat = useRoomStore((s) => s.chat);
  const members = useRoomStore((s) => s.members);
  const { user } = useAuth();
  const [text, setText] = useState("");
  const [closed, setClosed] = useState(false);
  const [active, setActive] = useState(0);
  const listRef = useRef<HTMLDivElement>(null);
  const stickToBottom = useRef(true);

  // Đang đọc tin cũ (cuộn lên) thì không kéo xuống khi có tin mới
  useEffect(() => {
    const el = listRef.current;
    if (el && stickToBottom.current) el.scrollTop = el.scrollHeight;
  }, [chat]);

  const myHandle = members.find((m) => m.userId === user?.sub)?.handle;

  // Đang gõ dở một @... ở cuối ô nhập thì gợi ý các người trong phòng
  const query = text.match(/(?:^|\s)@([a-z0-9_.-]*)$/i)?.[1]?.toLowerCase() ?? null;
  const suggestions =
    query === null || closed
      ? []
      : members
          .filter(
            (m) =>
              m.handle &&
              m.userId !== user?.sub &&
              (m.handle.startsWith(query) || m.name.toLowerCase().includes(query))
          )
          .slice(0, 5);

  useEffect(() => setActive(0), [query]);

  const pick = (member: RoomMember) => {
    setText((t) => t.replace(/@([a-z0-9_.-]*)$/i, `@${member.handle} `).slice(0, MAX_LENGTH));
  };

  const onKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (suggestions.length === 0) return;
    if (e.key === "ArrowDown" || e.key === "ArrowUp") {
      e.preventDefault();
      const step = e.key === "ArrowDown" ? 1 : suggestions.length - 1;
      setActive((a) => (a + step) % suggestions.length);
    } else if (e.key === "Enter" || e.key === "Tab") {
      e.preventDefault();
      pick(suggestions[Math.min(active, suggestions.length - 1)]);
    } else if (e.key === "Escape") {
      setClosed(true);
    }
  };

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
        {chat.map((m) => {
          const mentionsMe = !!myHandle && m.userId !== user?.sub && mentionsHandle(m.content, myHandle);
          return m.type === "SYSTEM" ? (
            <p key={m.id} className="text-center text-xs text-gray-500">
              {m.content}
            </p>
          ) : (
            <div
              key={m.id}
              className={`-mx-2 flex items-start gap-2 rounded-lg px-2 py-1 ${mentionsMe ? "bg-violet-100" : ""}`}
            >
              <Avatar className="size-7">
                <AvatarImage src={m.avatar ?? undefined} alt="" />
                <AvatarFallback>{(m.name ?? "?").slice(0, 1).toUpperCase()}</AvatarFallback>
              </Avatar>
              <div className="min-w-0 text-sm">
                <span className="mr-2 font-semibold text-gray-700" title={m.handle ? `@${m.handle}` : undefined}>
                  {m.name}
                  {m.handle && <span className="ml-1 text-xs font-normal text-gray-500">@{m.handle}</span>}
                </span>
                <LinkifiedText text={m.content} className="break-words" />
              </div>
            </div>
          );
        })}
      </div>
      <form onSubmit={submit} className="relative flex items-center gap-2 border-t p-3">
        {suggestions.length > 0 && (
          <ul role="listbox" className="absolute bottom-full left-3 right-3 mb-1 overflow-hidden rounded-lg border bg-white shadow-lg">
            {suggestions.map((m, i) => (
              <li key={m.userId} role="option" aria-selected={i === active}>
                <button
                  type="button"
                  // mousedown thay vì click để ô nhập không mất focus
                  onMouseDown={(e) => {
                    e.preventDefault();
                    pick(m);
                  }}
                  className={`flex w-full items-center gap-2 px-3 py-2 text-left text-sm ${i === active ? "bg-gray-100" : "hover:bg-gray-50"}`}
                >
                  <Avatar className="size-6">
                    <AvatarImage src={m.avatar ?? undefined} alt="" />
                    <AvatarFallback>{m.name.slice(0, 1).toUpperCase()}</AvatarFallback>
                  </Avatar>
                  <span className="truncate font-medium">{m.name}</span>
                  <span className="text-xs text-violet-600">@{m.handle}</span>
                </button>
              </li>
            ))}
          </ul>
        )}
        <input
          value={text}
          onChange={(e) => {
            setText(e.target.value);
            setClosed(false);
          }}
          onKeyDown={onKeyDown}
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
