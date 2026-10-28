"use client";

import { useState } from "react";
import Image from "next/image";
import Link from "next/link";
import { BarChart3, ExternalLink, Link as LinkIcon, MoreVertical, Play, Trash2 } from "lucide-react";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { useCopyToClipboard } from "@/lib/clipboard";
import { toastEmitter } from "@/lib/toastEmitter";
import { cn } from "@/lib/utils";
import { formatLiveDuration, liveUrl } from "@/modules/live/lib/format";
import type { MyPastLive } from "@/modules/live/types";

/** Còn chờ trình duyệt tải bản ghi lên: xoá bây giờ thì bản ghi mất, nên chưa cho xoá. */
export const waitingForUpload = (live: MyPastLive) => live.recordingStatus === "STOPPING";

/** Trạng thái video xem lại của một buổi: có video (bấm xem), đang chờ tải lên, không ghi hình, hoặc không có video. */
export function ReplayStatus({ live, className }: { live: MyPastLive; className?: string }) {
  if (live.replayVideoId) {
    return (
      <Link href={`/watch/${live.replayVideoId}`} className={cn("font-medium text-blue-700 hover:underline", className)}>
        Có video xem lại
      </Link>
    );
  }
  if (waitingForUpload(live)) {
    return <span className={cn("text-amber-700", className)}>Đang chờ tải bản ghi lên…</span>;
  }
  return (
    <span className={cn("text-gray-500", className)} title={live.recordingMessage ?? undefined}>
      {live.recordingEnabled ? "Không có video xem lại" : "Không ghi hình"}
    </span>
  );
}

const actionClass =
  "flex h-9 w-9 items-center justify-center rounded-full bg-black/70 text-white transition-colors hover:bg-black";

/** Thẻ một buổi đã phát trong trang Nội dung (Studio), cùng dáng và cách thao tác với thẻ video. */
export function LiveContentCard({ live, onDelete }: { live: MyPastLive; onDelete: (live: MyPastLive) => void }) {
  const [menuOpen, setMenuOpen] = useState(false);
  const [, copy] = useCopyToClipboard();
  const duration = formatLiveDuration(live.startedAt, live.endedAt);
  const page = `/live/${live.id}`;

  const copyLink = async () => {
    if (await copy(liveUrl(live.id))) toastEmitter.success("Đã sao chép liên kết");
    else toastEmitter.error("Không sao chép được, hãy chép thủ công");
  };

  return (
    <div
      className={cn(
        "group relative w-60 shrink-0 rounded-xl p-2 transition-colors hover:bg-gray-100",
        menuOpen && "bg-gray-100"
      )}
    >
      <Link href={page} className="block w-full text-left">
        <div className="relative aspect-video overflow-hidden rounded-lg bg-neutral-900">
          {live.thumbnailUrl && (
            <Image src={live.thumbnailUrl} alt={live.title} fill sizes="224px" className="object-cover" />
          )}
          {duration && (
            <span className="absolute bottom-1.5 right-1.5 rounded bg-black/80 px-1 text-xs font-medium text-white">
              {duration}
            </span>
          )}
        </div>
        <p className="mt-2 line-clamp-2 text-sm font-medium" title={live.title}>
          {live.title}
        </p>
        <p className="mt-1 truncate text-xs text-gray-600">
          Đã phát
          {live.endedAt && ` • ${new Date(live.endedAt).toLocaleDateString("vi-VN")}`}
        </p>
        <p className="mt-1 flex items-center gap-3 text-xs text-gray-600">
          <span className="flex items-center gap-1" title="Người xem cao nhất">
            <BarChart3 className="h-3.5 w-3.5" /> {live.peakViewers.toLocaleString("vi-VN")}
          </span>
        </p>
      </Link>
      <p className="mt-1 px-0.5 text-xs">
        <ReplayStatus live={live} />
      </p>

      {/* Nút thao tác khi rê chuột (vẫn hiện khi menu ⋮ đang mở) */}
      <div
        className={cn(
          "absolute left-0 right-0 top-4 flex justify-center gap-2 opacity-0 transition-opacity group-hover:opacity-100 focus-within:opacity-100",
          menuOpen && "opacity-100"
        )}
      >
        <Link href={page} target="_blank" title="Xem trang buổi phát" className={actionClass}>
          <ExternalLink className="h-4 w-4" />
          <span className="sr-only">Xem trang buổi phát</span>
        </Link>
        {live.replayVideoId && (
          <Link href={`/watch/${live.replayVideoId}`} target="_blank" title="Xem video xem lại" className={actionClass}>
            <Play className="h-4 w-4" />
            <span className="sr-only">Xem video xem lại</span>
          </Link>
        )}
        <DropdownMenu open={menuOpen} onOpenChange={setMenuOpen}>
          <DropdownMenuTrigger asChild>
            <button type="button" title="Tuỳ chọn" className={actionClass}>
              <MoreVertical className="h-4 w-4" />
              <span className="sr-only">Tuỳ chọn</span>
            </button>
          </DropdownMenuTrigger>
          <DropdownMenuContent align="start">
            <DropdownMenuItem onSelect={copyLink}>
              <LinkIcon className="h-4 w-4" /> Sao chép đường liên kết
            </DropdownMenuItem>
            <DropdownMenuSeparator />
            <DropdownMenuItem
              onSelect={() => onDelete(live)}
              disabled={waitingForUpload(live)}
              className="text-red-600 focus:text-red-600"
            >
              <Trash2 className="h-4 w-4" /> Xoá vĩnh viễn
            </DropdownMenuItem>
          </DropdownMenuContent>
        </DropdownMenu>
      </div>
    </div>
  );
}
