"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { API_PREFIX } from "@/constants/appConstant";
import { apiFetch } from "@/lib/apiFetch";
import type { LiveChatMessage, LiveEvent, LiveStatus, PollView } from "../types";
import { createLiveSocket, type LiveSocket } from "./liveSocket";

/** Giữ tối đa chừng này tin trong trình duyệt (backend giữ 100). */
const MAX_MESSAGES = 200;
/** Poll vừa đóng vẫn hiện thêm chừng này mili-giây rồi ẩn. */
const CLOSED_POLL_VISIBLE_MS = 60_000;

export interface LiveChannel {
  connected: boolean;
  /** Các trường dưới đây là null cho tới khi nhận tin đầu tiên; lúc đó dùng số liệu từ GET /api/live/{id}. */
  viewers: number | null;
  status: LiveStatus | null;
  chatEnabled: boolean | null;
  thumbnailUrl: string | null;
  /** undefined: chưa nhận tin Poll nào (dùng Poll trong GET /api/live/{id}); null: không còn Poll nào để hiện. */
  poll: PollView | null | undefined;
  messages: LiveChatMessage[];
  /** Lỗi riêng của người đăng nhập (vd. nhắn quá nhanh); tự xoá khi gọi clearError. */
  error: string | null;
  clearError: () => void;
  sendChat: (text: string) => void;
  deleteMessage: (messageId: string) => void;
  /** Ghi lại phiếu của chính mình vào Poll hiện tại (tin POLL của cả buổi không mang lựa chọn riêng). */
  setMyVote: (poll: PollView) => void;
}

function mergeMessages(current: LiveChatMessage[], incoming: LiveChatMessage[]): LiveChatMessage[] {
  const known = new Set(current.map((m) => m.id));
  const added = incoming.filter((m) => !known.has(m.id));
  if (added.length === 0) return current;
  return [...current, ...added].sort((a, b) => a.at - b.at).slice(-MAX_MESSAGES);
}

/**
 * Nghe một buổi phát qua STOMP. Đăng ký topic trước, rồi tải các tin chat gần nhất qua REST và gộp (bỏ tin trùng theo
 * id), nên không rơi mất tin nào giữa hai bước. Dùng chung cho khách và người đăng nhập.
 */
export function useLiveChannel(streamId: string | undefined, authenticated: boolean): LiveChannel {
  const [connected, setConnected] = useState(false);
  const [viewers, setViewers] = useState<number | null>(null);
  const [status, setStatus] = useState<LiveStatus | null>(null);
  const [chatEnabled, setChatEnabled] = useState<boolean | null>(null);
  const [thumbnailUrl, setThumbnailUrl] = useState<string | null>(null);
  const [poll, setPoll] = useState<PollView | null | undefined>(undefined);
  const [messages, setMessages] = useState<LiveChatMessage[]>([]);
  const [error, setError] = useState<string | null>(null);
  const socketRef = useRef<LiveSocket | null>(null);
  const hideTimerRef = useRef<number | null>(null);

  useEffect(() => {
    if (!streamId) return;
    let cancelled = false;

    const clearHideTimer = () => {
      if (hideTimerRef.current !== null) {
        window.clearTimeout(hideTimerRef.current);
        hideTimerRef.current = null;
      }
    };

    const loadRecentChat = async () => {
      try {
        const res = await apiFetch(`${API_PREFIX}/live/${streamId}/chat`);
        if (!res.ok || cancelled) return;
        const recent = (await res.json()) as LiveChatMessage[];
        setMessages((current) => mergeMessages(current, recent));
      } catch {
        // chat cũ không tải được: vẫn nhận tin mới qua STOMP
      }
    };

    const onEvent = (event: LiveEvent) => {
      switch (event.type) {
        case "CHAT":
          if (event.message) {
            const message = event.message;
            setMessages((current) => mergeMessages(current, [message]));
          }
          break;
        case "CHAT_DELETED":
          setMessages((current) => current.filter((m) => m.id !== event.messageId));
          break;
        case "CHAT_STATE":
          if (typeof event.chatEnabled === "boolean") setChatEnabled(event.chatEnabled);
          break;
        case "VIEWERS":
          if (typeof event.viewers === "number") setViewers(event.viewers);
          break;
        case "STATUS":
          if (event.status) setStatus(event.status);
          break;
        case "THUMBNAIL":
          if (event.thumbnailUrl) setThumbnailUrl(event.thumbnailUrl);
          break;
        case "POLL": {
          const incoming = event.poll;
          if (!incoming) break;
          clearHideTimer();
          // Tin của cả buổi không mang phiếu của riêng mình: giữ lại phiếu đã biết của cùng Poll
          setPoll((prev) => ({ ...incoming, myVote: prev && prev.id === incoming.id ? prev.myVote : incoming.myVote }));
          if (!incoming.open) {
            hideTimerRef.current = window.setTimeout(() => setPoll(null), CLOSED_POLL_VISIBLE_MS);
          }
          break;
        }
        case "ERROR":
          if (event.text) setError(event.text);
          break;
      }
    };

    const socket = createLiveSocket(
      streamId,
      {
        onEvent,
        onConnected: () => {
          setConnected(true);
          void loadRecentChat();
        },
        onDisconnected: () => setConnected(false),
      },
      authenticated
    );
    socketRef.current = socket;

    return () => {
      cancelled = true;
      clearHideTimer();
      socketRef.current = null;
      socket.stop();
      setConnected(false);
    };
  }, [streamId, authenticated]);

  const sendChat = useCallback((text: string) => socketRef.current?.send("chat", { text }), []);
  const deleteMessage = useCallback(
    (messageId: string) => socketRef.current?.send("chat/delete", { messageId }),
    []
  );
  const clearError = useCallback(() => setError(null), []);
  const setMyVote = useCallback((voted: PollView) => setPoll(voted), []);

  return { connected, viewers, status, chatEnabled, thumbnailUrl, poll, messages, error, clearError, sendChat, deleteMessage, setMyVote };
}
