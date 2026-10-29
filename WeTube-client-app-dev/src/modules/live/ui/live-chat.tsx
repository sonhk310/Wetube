"use client";

import { useEffect, useRef, useState } from "react";
import { Send, Trash2 } from "lucide-react";
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { useAuth } from "@/contexts/auth-context";
import { cn } from "@/lib/utils";
import MemberBadge from "@/components/member-badge";
import PremiumBadge from "@/components/premium-badge";
import LinkifiedText from "@/modules/watch/components/linkified-text";
import type { LiveChannel } from "../lib/useLiveChannel";
import type { LiveChatMessage, LiveStatus } from "../types";

const MAX_LENGTH = 200;
const ERROR_VISIBLE_MS = 4000;

function Message({
  message,
  canDelete,
  onDelete,
}: {
  message: LiveChatMessage;
  canDelete: boolean;
  onDelete: () => void;
}) {
  const name = message.name ?? "Ẩn danh";
  return (
    <div className="group flex items-start gap-2 px-3 py-1">
      <Avatar className="mt-0.5 size-6 shrink-0">
        <AvatarImage src={message.avatar ?? undefined} alt={name} />
        <AvatarFallback className="text-[10px]">{name.charAt(0).toUpperCase()}</AvatarFallback>
      </Avatar>
      <p className="min-w-0 flex-1 break-words text-sm">
        <span
          className={cn(
            "mr-1.5 font-semibold",
            message.owner ? "rounded bg-blue-600 px-1 text-white" : "text-gray-500"
          )}
        >
          {name}
        </span>
        {message.premium && <PremiumBadge className="mr-1.5" />}
        {message.memberTier && <MemberBadge tier={message.memberTier} className="mr-1.5" />}
        {/* Văn bản thuần: React tự thoát ký tự, không dựng HTML từ nội dung chat */}
        <LinkifiedText text={message.text} seekable={false} className="inline" />
      </p>
      {canDelete ? (
        <button
          type="button"
          aria-label="Xoá tin nhắn"
          onClick={onDelete}
          className="shrink-0 rounded p-1 text-gray-400 opacity-0 hover:bg-gray-100 hover:text-red-600 group-hover:opacity-100 focus:opacity-100"
        >
          <Trash2 className="size-4" />
        </button>
      ) : null}
    </div>
  );
}

/**
 * Khung chat trực tiếp. Khách đọc được nhưng phải đăng nhập mới nhắn; chủ kênh (`canModerate`) xoá được tin.
 * `chatEnabled` / `status` là giá trị đã gộp giữa REST và tin STOMP.
 */
export default function LiveChat({
  channel,
  status,
  chatEnabled,
  canModerate,
  className,
}: {
  channel: LiveChannel;
  status: LiveStatus;
  chatEnabled: boolean;
  canModerate: boolean;
  className?: string;
}) {
  const { user, login } = useAuth();
  const [draft, setDraft] = useState("");
  const listRef = useRef<HTMLDivElement>(null);
  const stickToBottom = useRef(true);
  const { messages, error, clearError } = channel;

  useEffect(() => {
    const el = listRef.current;
    if (el && stickToBottom.current) el.scrollTop = el.scrollHeight;
  }, [messages]);

  useEffect(() => {
    if (!error) return;
    const timer = window.setTimeout(clearError, ERROR_VISIBLE_MS);
    return () => window.clearTimeout(timer);
  }, [error, clearError]);

  const send = () => {
    const text = draft.trim();
    if (!text) return;
    channel.sendChat(text);
    setDraft("");
    stickToBottom.current = true;
  };

  const notStarted = status === "CREATED";
  const closedMessage = !chatEnabled ? "Chat đã bị tắt." : notStarted ? "Chat sẽ mở khi buổi phát bắt đầu." : null;

  return (
    <div className={cn("flex min-h-0 flex-col overflow-hidden rounded-xl border border-gray-200 bg-white", className)}>
      <div className="border-b border-gray-200 px-3 py-2 text-sm font-semibold">Trò chuyện trực tiếp</div>
      <div
        ref={listRef}
        onScroll={(e) => {
          const el = e.currentTarget;
          stickToBottom.current = el.scrollHeight - el.scrollTop - el.clientHeight < 40;
        }}
        className="min-h-0 flex-1 overflow-y-auto py-2"
      >
        {closedMessage ? (
          <p className="px-3 py-6 text-center text-sm text-gray-500">{closedMessage}</p>
        ) : messages.length === 0 ? (
          <p className="px-3 py-6 text-center text-sm text-gray-500">Chưa có tin nhắn nào.</p>
        ) : (
          messages.map((m) => (
            <Message
              key={m.id}
              message={m}
              canDelete={canModerate}
              onDelete={() => channel.deleteMessage(m.id)}
            />
          ))
        )}
      </div>
      <div className="border-t border-gray-200 p-2">
        {closedMessage ? null : !user?.sub ? (
          <Button variant="secondary" className="w-full" onClick={login}>
            Đăng nhập để trò chuyện
          </Button>
        ) : (
          <>
            {error ? <p className="mb-1 text-xs text-red-600">{error}</p> : null}
            <div className="flex items-center gap-2">
              <Input
                value={draft}
                maxLength={MAX_LENGTH}
                onChange={(e) => setDraft(e.target.value)}
                onKeyDown={(e) => {
                  if (e.key === "Enter" && !e.nativeEvent.isComposing) {
                    e.preventDefault();
                    send();
                  }
                }}
                placeholder="Nhập tin nhắn…"
                aria-label="Tin nhắn"
              />
              <Button size="icon" aria-label="Gửi" disabled={!draft.trim() || !channel.connected} onClick={send}>
                <Send className="size-4" />
              </Button>
            </div>
            <p className="mt-1 text-right text-[11px] text-gray-400">
              {draft.length}/{MAX_LENGTH}
            </p>
          </>
        )}
      </div>
    </div>
  );
}
