"use client";

import { useState } from "react";
import Image from "next/image";
import Link from "next/link";
import { ListPlus, MoreVertical, Play, Share2, Trash2 } from "lucide-react";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import SaveToPlaylistDialog from "@/modules/watch/components/save-to-playlist-dialog";
import ShareDialog from "@/modules/watch/components/share-dialog";
import { useWatchProgress } from "@/lib/watchProgress";
import { cn, formatCompact, formatDuration, timeAgo } from "@/lib/utils";
import type { PlaylistVideoItem } from "../../types";

const FALLBACK_THUMBNAIL = "/images/thumbnail.png";

interface PlaylistVideoRowProps {
  index: number;
  video: PlaylistVideoItem;
  /** Tên danh sách đã qua playlistDisplayTitle, để hiện "Xoá khỏi {tên}". */
  playlistTitle: string;
  href: string;
  /** Video đang phát (khung danh sách trên trang xem). */
  active?: boolean;
  /** Dạng gọn cho khung danh sách trên trang xem. */
  compact?: boolean;
  /** Có thì menu có "Xoá khỏi {tên}" (chỉ chủ danh sách). */
  onRemove?: () => void;
}

/** Một video trong danh sách phát: số thứ tự, ảnh + thời lượng + thanh đã xem, tên, kênh, menu ⋮. */
export function PlaylistVideoRow({ index, video, playlistTitle, href, active, compact, onRemove }: PlaylistVideoRowProps) {
  const [saveOpen, setSaveOpen] = useState(false);
  const [shareOpen, setShareOpen] = useState(false);
  const watched = useWatchProgress(video.videoId) ?? 0;
  const progress = video.duration > 0 ? watched / video.duration : 0;

  return (
    <div
      className={cn(
        "group/row relative flex items-start gap-2 rounded-lg hover:bg-gray-100",
        compact ? "px-2 py-1.5" : "p-2",
        active && "bg-gray-200 hover:bg-gray-200"
      )}
    >
      <span
        className={cn(
          "flex shrink-0 justify-center self-center text-gray-500",
          compact ? "w-5 text-xs" : "w-8 text-sm"
        )}
      >
        {active ? <Play className="size-3" fill="currentColor" /> : index + 1}
      </span>
      <Link
        href={href}
        className={cn(
          "relative aspect-video shrink-0 overflow-hidden rounded-lg bg-gray-200",
          compact ? "w-28" : "w-40 sm:w-44"
        )}
      >
        <Image
          src={video.thumbnailUrl || FALLBACK_THUMBNAIL}
          alt={video.videoTitle}
          fill
          sizes={compact ? "112px" : "176px"}
          className="object-cover"
        />
        <span className="absolute bottom-1 right-1 rounded bg-black/80 px-1 text-xs font-medium text-white">
          {formatDuration(video.duration || 0)}
        </span>
        {progress > 0 && (
          <span className="absolute inset-x-0 bottom-0 h-1 bg-gray-400/60">
            <span className="block h-full bg-red-600" style={{ width: `${Math.min(100, progress * 100)}%` }} />
          </span>
        )}
      </Link>
      <Link href={href} className="min-w-0 flex-1 pr-6">
        <h3 className={cn("line-clamp-2 font-medium text-gray-900", compact ? "text-sm" : "text-base")}>
          {video.videoTitle}
        </h3>
        <p className={cn("text-gray-600", compact ? "text-xs" : "text-sm")}>
          {video.channelName}
          {!compact && ` · ${formatCompact(video.totalView)} lượt xem · ${timeAgo(video.createdAt)}`}
        </p>
      </Link>
      <DropdownMenu>
        <DropdownMenuTrigger
          aria-label="Tác vụ khác"
          className="absolute right-1 top-2 rounded-full p-1.5 opacity-0 hover:bg-gray-200 focus-visible:opacity-100 group-hover/row:opacity-100 data-[state=open]:opacity-100 max-md:opacity-100"
        >
          <MoreVertical className="size-4" />
        </DropdownMenuTrigger>
        <DropdownMenuContent align="end">
          <DropdownMenuItem onSelect={() => setSaveOpen(true)} className="cursor-pointer gap-3 py-2.5">
            <ListPlus className="size-5" /> Lưu vào danh sách phát
          </DropdownMenuItem>
          {onRemove && (
            <DropdownMenuItem onSelect={onRemove} className="cursor-pointer gap-3 py-2.5">
              <Trash2 className="size-5" /> Xoá khỏi {playlistTitle}
            </DropdownMenuItem>
          )}
          <DropdownMenuItem onSelect={() => setShareOpen(true)} className="cursor-pointer gap-3 py-2.5">
            <Share2 className="size-5" /> Chia sẻ
          </DropdownMenuItem>
        </DropdownMenuContent>
      </DropdownMenu>
      <SaveToPlaylistDialog videoId={video.videoId} open={saveOpen} onOpenChange={setSaveOpen} />
      <ShareDialog
        videoId={video.videoId}
        title={video.videoTitle}
        currentTime={0}
        open={shareOpen}
        onOpenChange={setShareOpen}
      />
    </div>
  );
}
