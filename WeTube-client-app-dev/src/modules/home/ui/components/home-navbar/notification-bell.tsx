"use client";

import { useCallback, useEffect, useState } from "react";
import Image from "next/image";
import { useRouter } from "next/navigation";
import { Bell } from "lucide-react";
import { Button } from "@/components/ui/button";
import { DropdownMenu, DropdownMenuContent, DropdownMenuTrigger } from "@/components/ui/dropdown-menu";
import { API_PREFIX } from "@/constants/appConstant";
import { apiFetch } from "@/lib/apiFetch";
import { cn, timeAgo } from "@/lib/utils";

type NotificationType = "NEW_VIDEO" | "COMMENT_REPLY" | "VIDEO_COMMENT" | "COMMENT_HEART";

interface NotificationItem {
  id: string;
  type: NotificationType;
  read: boolean;
  createdAt: string;
  actorName: string | null;
  actorPicture: string | null;
  videoId: string | null;
  videoTitle: string | null;
  thumbnailUrl: string | null;
  commentId: string | null;
  text: string | null;
}

interface NotificationPage {
  items: NotificationItem[];
  hasMore: boolean;
  unread: number;
}

/** Hỏi số thông báo chưa đọc mỗi khoảng này (và khi quay lại tab). */
const POLL_MS = 60_000;
const PAGE_SIZE = 20;

function message(n: NotificationItem) {
  const who = n.actorName ?? "Ai đó";
  switch (n.type) {
    case "NEW_VIDEO":
      return `${who} đã tải lên: ${n.videoTitle ?? "video mới"}`;
    case "COMMENT_REPLY":
      return `${who} đã trả lời bình luận của bạn: "${n.text ?? ""}"`;
    case "VIDEO_COMMENT":
      return `${who} đã bình luận về video "${n.videoTitle ?? ""}": "${n.text ?? ""}"`;
    case "COMMENT_HEART":
      return `${who} đã thả ❤️ bình luận của bạn: "${n.text ?? ""}"`;
  }
}

/** Chuông thông báo trên thanh trên cùng (người đã đăng nhập). */
export default function NotificationBell() {
  const router = useRouter();
  const [open, setOpen] = useState(false);
  const [unread, setUnread] = useState(0);
  const [items, setItems] = useState<NotificationItem[]>([]);
  const [page, setPage] = useState(0);
  const [hasMore, setHasMore] = useState(false);
  const [loading, setLoading] = useState(false);

  const refreshCount = useCallback(async () => {
    try {
      const res = await apiFetch(`${API_PREFIX}/notifications/unread-count`);
      if (res.ok) setUnread((await res.json()).count ?? 0);
    } catch {
      // mạng lỗi: thử lại ở lần sau
    }
  }, []);

  useEffect(() => {
    void refreshCount();
    const timer = window.setInterval(refreshCount, POLL_MS);
    const onVisible = () => {
      if (document.visibilityState === "visible") void refreshCount();
    };
    document.addEventListener("visibilitychange", onVisible);
    return () => {
      window.clearInterval(timer);
      document.removeEventListener("visibilitychange", onVisible);
    };
  }, [refreshCount]);

  const loadPage = useCallback(async (nextPage: number) => {
    setLoading(true);
    try {
      const res = await apiFetch(`${API_PREFIX}/notifications?page=${nextPage}&size=${PAGE_SIZE}`);
      if (!res.ok) return;
      const data: NotificationPage = await res.json();
      setItems((prev) => (nextPage === 0 ? data.items : [...prev, ...data.items]));
      setHasMore(data.hasMore);
      setUnread(data.unread);
      setPage(nextPage);
    } catch (err) {
      console.error("Không tải được thông báo:", err);
    } finally {
      setLoading(false);
    }
  }, []);

  const handleOpenChange = (next: boolean) => {
    setOpen(next);
    if (next) void loadPage(0);
  };

  const markAllRead = async () => {
    setItems((prev) => prev.map((n) => ({ ...n, read: true })));
    setUnread(0);
    await apiFetch(`${API_PREFIX}/notifications/read-all`, { method: "POST" }).catch(() => {});
  };

  const openItem = (n: NotificationItem) => {
    if (!n.read) {
      setItems((prev) => prev.map((x) => (x.id === n.id ? { ...x, read: true } : x)));
      setUnread((u) => Math.max(0, u - 1));
      apiFetch(`${API_PREFIX}/notifications/${n.id}/read`, { method: "POST" }).catch(() => {});
    }
    setOpen(false);
    if (n.videoId) router.push(`/watch/${n.videoId}`);
  };

  return (
    <DropdownMenu open={open} onOpenChange={handleOpenChange}>
      <DropdownMenuTrigger asChild>
        <Button
          variant="ghost"
          size="icon"
          aria-label={unread > 0 ? `Thông báo (${unread} chưa đọc)` : "Thông báo"}
          className="relative h-10 w-10 rounded-full"
        >
          <Bell className="size-6" />
          {unread > 0 && (
            <span className="absolute top-0.5 right-0.5 min-w-[18px] rounded-full bg-red-600 px-1 text-center text-[11px] leading-[18px] font-semibold text-white">
              {unread > 9 ? "9+" : unread}
            </span>
          )}
        </Button>
      </DropdownMenuTrigger>

      <DropdownMenuContent align="end" sideOffset={8} className="w-[min(480px,calc(100vw-1rem))] p-0">
        <div className="flex items-center justify-between border-b px-4 py-3">
          <p className="font-semibold">Thông báo</p>
          {unread > 0 && (
            <button type="button" onClick={markAllRead} className="text-sm text-blue-600 hover:underline">
              Đánh dấu tất cả đã đọc
            </button>
          )}
        </div>

        <div className="max-h-[70vh] overflow-y-auto py-1">
          {items.length === 0 && !loading && (
            <p className="px-4 py-10 text-center text-sm text-gray-500">
              Bạn chưa có thông báo nào. Đăng ký kênh để biết khi họ có video mới.
            </p>
          )}

          {items.map((n) => (
            <button
              key={n.id}
              type="button"
              onClick={() => openItem(n)}
              className={cn(
                "flex w-full items-start gap-3 px-4 py-3 text-left hover:bg-gray-100",
                !n.read && "bg-blue-50/60"
              )}
            >
              <span className={cn("mt-4 h-1.5 w-1.5 shrink-0 rounded-full", n.read ? "bg-transparent" : "bg-blue-600")} />
              {n.actorPicture ? (
                <Image
                  src={n.actorPicture}
                  alt=""
                  width={40}
                  height={40}
                  className="h-10 w-10 shrink-0 rounded-full object-cover"
                />
              ) : (
                <span className="h-10 w-10 shrink-0 rounded-full bg-gray-200" />
              )}
              <span className="min-w-0 flex-1">
                <span className="line-clamp-3 text-sm">{message(n)}</span>
                <span className="mt-1 block text-xs text-gray-500">{timeAgo(n.createdAt)}</span>
              </span>
              {n.thumbnailUrl && (
                <span
                  className="aspect-video w-24 shrink-0 rounded-md bg-gray-200 bg-cover bg-center"
                  style={{ backgroundImage: `url(${n.thumbnailUrl})` }}
                />
              )}
            </button>
          ))}

          {loading && <p className="px-4 py-3 text-center text-sm text-gray-500">Đang tải…</p>}

          {hasMore && !loading && (
            <button
              type="button"
              onClick={() => loadPage(page + 1)}
              className="w-full px-4 py-3 text-center text-sm font-medium text-blue-600 hover:bg-gray-100"
            >
              Xem thêm
            </button>
          )}
        </div>
      </DropdownMenuContent>
    </DropdownMenu>
  );
}
