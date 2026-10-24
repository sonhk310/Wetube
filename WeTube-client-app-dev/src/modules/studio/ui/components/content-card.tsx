"use client";

import Image from "next/image";
import Link from "next/link";
import { BarChart3, Ban, FileText, Globe2, Link as LinkIcon, Lock, Trash2 } from "lucide-react";
import { cn, formatDuration } from "@/lib/utils";
import { videoImageUrl } from "@/lib/videoMedia";
import type { VideoFromPlaylist } from "@/types/playlistSummary";

const STATUS: Record<string, { label: string; icon: typeof Globe2; className?: string }> = {
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

/** Thẻ video trong trang Nội dung (Studio); bản nháp có ảnh tối lại. */
export function ContentCard({ video, vertical, onDelete }: ContentCardProps) {
  const status = STATUS[video.videoStatus ?? "ACTIVE"] ?? STATUS.ACTIVE;
  const StatusIcon = status.icon;
  const isDraft = video.videoStatus === "DRAFT";
  const image = videoImageUrl(video.thumbnailUrl, video.videoUrl);

  return (
    <div className={cn("group relative shrink-0", vertical ? "w-44" : "w-56")}>
      <Link href={`/studio/videos/${video.videoId}`} className="block">
        <div
          className={cn(
            "relative overflow-hidden rounded-xl bg-gray-200",
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
          <p className="mt-1 flex items-center gap-1 text-xs text-gray-600">
            <BarChart3 className="h-3.5 w-3.5" /> {video.totalView}
          </p>
        )}
      </Link>
      <button
        type="button"
        onClick={() => onDelete(video)}
        title={isDraft ? "Xoá video nháp" : "Xoá video"}
        className="absolute right-2 top-2 rounded-full bg-black/60 p-1.5 text-white opacity-0 transition-opacity hover:bg-black/80 focus:opacity-100 group-hover:opacity-100"
      >
        <Trash2 className="h-4 w-4" />
        <span className="sr-only">Xoá</span>
      </button>
    </div>
  );
}
