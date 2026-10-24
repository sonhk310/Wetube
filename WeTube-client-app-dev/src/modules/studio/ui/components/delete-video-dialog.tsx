"use client";

import { useEffect, useState } from "react";
import Image from "next/image";
import { Button } from "@/components/ui/button";
import { Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { formatDuration } from "@/lib/utils";
import { videoDownloadUrl, videoImageUrl } from "@/lib/videoMedia";
import type { VideoFromPlaylist } from "@/types/playlistSummary";

interface DeleteVideoDialogProps {
  video: VideoFromPlaylist | null;
  deleting: boolean;
  onCancel: () => void;
  onConfirm: () => void;
}

/** Hỏi lại trước khi xoá, như YouTube Studio: phải tích ô xác nhận mới bấm xoá được. */
export function DeleteVideoDialog({ video, deleting, onCancel, onConfirm }: DeleteVideoDialogProps) {
  const [confirmed, setConfirmed] = useState(false);
  useEffect(() => setConfirmed(false), [video?.videoId]);

  const isDraft = video?.videoStatus === "DRAFT";
  const image = video ? videoImageUrl(video.thumbnailUrl, video.videoUrl) : "";

  return (
    <Dialog open={!!video} onOpenChange={(open) => !open && !deleting && onCancel()}>
      <DialogContent className="grid-cols-[minmax(0,1fr)] sm:max-w-xl rounded-2xl">
        <DialogHeader>
          <DialogTitle className="text-xl">{isDraft ? "Xoá video nháp này?" : "Xoá vĩnh viễn video này?"}</DialogTitle>
        </DialogHeader>
        {video && (
          <div className="flex gap-4 rounded-lg bg-gray-100 p-3">
            <div className="relative aspect-video w-36 shrink-0 overflow-hidden rounded-md bg-gray-300">
              {image && (
                <Image
                  src={image}
                  alt={video.videoTitle}
                  fill
                  sizes="144px"
                  className={isDraft ? "object-cover brightness-50" : "object-cover"}
                />
              )}
              {video.duration > 0 && (
                <span className="absolute bottom-1 right-1 rounded bg-black/80 px-1 text-xs text-white">
                  {formatDuration(video.duration)}
                </span>
              )}
            </div>
            <div className="min-w-0 text-sm">
              <p className="line-clamp-2 font-medium">{video.videoTitle}</p>
              <p className="text-gray-600">
                Ngày tải lên: {new Date(video.createdAt).toLocaleDateString("vi-VN")}
              </p>
              {!isDraft && <p className="text-gray-600">{video.totalView} lượt xem</p>}
            </div>
          </div>
        )}
        <label className="flex cursor-pointer items-start gap-3 text-sm">
          <input
            type="checkbox"
            checked={confirmed}
            onChange={(e) => setConfirmed(e.target.checked)}
            className="mt-0.5 h-4 w-4 accent-red-600"
          />
          <span>
            {isDraft
              ? "Tôi hiểu rằng video nháp này và tệp đã tải lên sẽ bị xoá, không khôi phục được."
              : "Tôi hiểu rằng việc xoá video này có hiệu lực vĩnh viễn và tôi sẽ không còn truy cập được vào video cũng như số liệu của video."}
          </span>
        </label>
        <DialogFooter className="flex-row items-center sm:justify-between">
          {video && (
            <Button asChild variant="secondary" className="rounded-full">
              <a href={videoDownloadUrl(video.videoUrl)} target="_blank" rel="noreferrer">
                Tải video xuống
              </a>
            </Button>
          )}
          <div className="ml-auto flex gap-2">
            <Button variant="ghost" className="rounded-full" onClick={onCancel} disabled={deleting}>
              Huỷ
            </Button>
            <Button
              variant="destructive"
              className="rounded-full"
              onClick={onConfirm}
              disabled={!confirmed || deleting}
            >
              {deleting ? "Đang xoá…" : isDraft ? "Xoá video nháp" : "Xoá vĩnh viễn"}
            </Button>
          </div>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
