"use client";

import { type ReactNode, useState } from "react";
import Image from "next/image";
import Link from "next/link";
import {
  BarChart3,
  Ban,
  Download,
  ExternalLink,
  FileText,
  Globe2,
  Link as LinkIcon,
  Lock,
  MessageSquare,
  MoreVertical,
  Pencil,
  Trash2,
} from "lucide-react";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { useCopyToClipboard } from "@/lib/clipboard";
import { toastEmitter } from "@/lib/toastEmitter";
import { cn, formatDuration } from "@/lib/utils";
import { videoDownloadUrl, videoImageUrl } from "@/lib/videoMedia";
import type { VideoFromPlaylist } from "@/types/playlistSummary";
import { DraftEditDialog } from "./draft-edit-dialog";

export const STATUS: Record<string, { label: string; icon: typeof Globe2; className?: string }> = {
  ACTIVE: { label: "Công khai", icon: Globe2 },
  PUBLIC: { label: "Công khai", icon: Globe2 },
  UNLISTED: { label: "Không công khai", icon: LinkIcon },
  PRIVATE: { label: "Riêng tư", icon: Lock },
  INACTIVE: { label: "Bị ẩn", icon: Ban, className: "text-red-600" },
  DRAFT: { label: "Bản nháp", icon: FileText },
};

interface ContentCardProps {
  video: VideoFromPlaylist;
  vertical: boolean;
  onDelete: (video: VideoFromPlaylist) => void;
}

const actionClass =
  "flex h-9 w-9 items-center justify-center rounded-full bg-black/70 text-white transition-colors hover:bg-black";

/** Video đã đăng: link tới trang Chi tiết; bản nháp: nút mở lại hộp thoại tải lên (như YouTube). */
function EditTarget({
  isDraft,
  href,
  onOpenDraft,
  className,
  title,
  children,
}: {
  isDraft: boolean;
  href: string;
  onOpenDraft: () => void;
  className: string;
  title?: string;
  children: ReactNode;
}) {
  return isDraft ? (
    <button type="button" onClick={onOpenDraft} title={title} className={className}>
      {children}
    </button>
  ) : (
    <Link href={href} title={title} className={className}>
      {children}
    </Link>
  );
}

/**
 * Thẻ video trong trang Nội dung (Studio). Rê chuột: nền thẻ sáng lên và hiện các nút thao tác
 * (video: hàng ngang phía trên ảnh; Shorts: cột dọc bên phải). Bản nháp có ảnh tối lại.
 */
export function ContentCard({ video, vertical, onDelete }: ContentCardProps) {
  const [menuOpen, setMenuOpen] = useState(false);
  const [, copy] = useCopyToClipboard();
  const status = STATUS[video.videoStatus ?? "ACTIVE"] ?? STATUS.ACTIVE;
  const StatusIcon = status.icon;
  const isDraft = video.videoStatus === "DRAFT";
  const image = videoImageUrl(video.thumbnailUrl, video.videoUrl);
  const [draftOpen, setDraftOpen] = useState(false);
  const editHref = `/studio/videos/${video.videoId}`;
  const watchHref = `/${vertical ? "shorts" : "watch"}/${video.videoId}`;

  const copyLink = async () => {
    if (await copy(`${window.location.origin}${watchHref}`)) toastEmitter.success("Đã sao chép liên kết");
    else toastEmitter.error("Không sao chép được, hãy chép thủ công");
  };

  return (
    <div
      className={cn(
        "group relative shrink-0 rounded-xl p-2 transition-colors hover:bg-gray-100",
        menuOpen && "bg-gray-100",
        vertical ? "w-48" : "w-60"
      )}
    >
      <EditTarget isDraft={isDraft} href={editHref} onOpenDraft={() => setDraftOpen(true)} className="block w-full text-left">
        <div
          className={cn(
            "relative overflow-hidden rounded-lg bg-gray-200",
            vertical ? "aspect-[9/16]" : "aspect-video"
          )}
        >
          {image && (
            <Image
              src={image}
              alt={video.videoTitle}
              fill
              sizes={vertical ? "176px" : "224px"}
              className={cn("object-cover", isDraft && "brightness-50")}
            />
          )}
          {video.duration > 0 && (
            <span className="absolute bottom-1.5 right-1.5 rounded bg-black/80 px-1 text-xs font-medium text-white">
              {formatDuration(video.duration)}
            </span>
          )}
        </div>
        <p className="mt-2 line-clamp-2 text-sm font-medium" title={video.videoTitle}>
          {video.videoTitle}
        </p>
        <p className={cn("mt-1 flex items-center gap-1 text-xs text-gray-600", status.className)}>
          <StatusIcon className="h-3.5 w-3.5 shrink-0" />
          <span className="truncate">
            {status.label}
            {!isDraft && ` • ${new Date(video.createdAt).toLocaleDateString("vi-VN")}`}
          </span>
        </p>
        {!isDraft && (
          <p className="mt-1 flex items-center gap-3 text-xs text-gray-600">
            <span className="flex items-center gap-1" title="Lượt xem">
              <BarChart3 className="h-3.5 w-3.5" /> {video.totalView}
            </span>
            <span className="flex items-center gap-1" title="Bình luận">
              <MessageSquare className="h-3.5 w-3.5" /> {video.commentCount ?? 0}
            </span>
          </p>
        )}
      </EditTarget>

      {/* Nút thao tác khi rê chuột (vẫn hiện khi menu ⋮ đang mở) */}
      <div
        className={cn(
          "absolute flex gap-2 opacity-0 transition-opacity group-hover:opacity-100 focus-within:opacity-100",
          menuOpen && "opacity-100",
          vertical ? "right-3.5 top-3.5 flex-col" : "left-0 right-0 top-4 justify-center"
        )}
      >
        <EditTarget isDraft={isDraft} href={editHref} onOpenDraft={() => setDraftOpen(true)} title="Chi tiết" className={actionClass}>
          <Pencil className="h-4 w-4" />
          <span className="sr-only">Chi tiết</span>
        </EditTarget>
        {!isDraft && (
          <Link href={watchHref} target="_blank" title="Xem trên WeTube" className={actionClass}>
            <ExternalLink className="h-4 w-4" />
            <span className="sr-only">Xem trên WeTube</span>
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
            <DropdownMenuItem asChild>
              <a href={videoDownloadUrl(video.videoUrl)} target="_blank" rel="noreferrer">
                <Download className="h-4 w-4" /> Tải video xuống
              </a>
            </DropdownMenuItem>
            <DropdownMenuSeparator />
            <DropdownMenuItem onSelect={() => onDelete(video)} className="text-red-600 focus:text-red-600">
              <Trash2 className="h-4 w-4" /> {isDraft ? "Xoá video nháp" : "Xoá vĩnh viễn"}
            </DropdownMenuItem>
          </DropdownMenuContent>
        </DropdownMenu>
      </div>

      {isDraft && <DraftEditDialog videoId={video.videoId} open={draftOpen} onOpenChange={setDraftOpen} />}
    </div>
  );
}
