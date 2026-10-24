"use client";

import { useEffect, useRef, useState } from "react";
import Image from "next/image";
import Link from "next/link";
import { useProgressRouter } from "@/hooks/use-progress-router";
import {
  Clock,
  ListPlus,
  MoreVertical,
  Share2,
  Volume2,
  VolumeX,
} from "lucide-react";
import { toast } from "sonner";
import { useSaveInteractionMutation } from "@/app/api/interactionApi";
import { Skeleton } from "@/components/ui/skeleton";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { useAuth } from "@/contexts/auth-context";
import { cn, formatViews, timeAgo } from "@/lib/utils";
import { useWatchProgress } from "@/lib/watchProgress";
import { playlistService } from "@/modules/playlists/ui/list/playlist-API";
import { THUMBNAIL_FALLBACK } from "@/modules/videos/constants";
import { formatTime } from "@/modules/watch/components/video/format-time";
import SaveToPlaylistDialog from "@/modules/watch/components/save-to-playlist-dialog";
import ShareDialog from "@/modules/watch/components/share-dialog";
import { RecommendedVideoItem } from "@/types/video";

/** Rê chuột lâu chừng này mới phát xem trước (tránh tải video khi chỉ lướt qua). */
const PREVIEW_DELAY_MS = 600;

/** Thẻ video kiểu YouTube: rê chuột thì phát xem trước không tiếng, avatar / tên kênh dẫn tới kênh. */
const VideoCard = ({
  id,
  title,
  thumbnailUrl,
  videoUrl,
  totalView,
  createAt,
  name,
  duration,
  picture,
  historyDuration,
  channelId,
}: RecommendedVideoItem) => {
  const router = useProgressRouter();
  const { user } = useAuth();
  const [saveInteraction] = useSaveInteractionMutation();
  const watched = useWatchProgress(id) ?? historyDuration ?? 0;

  const watchHref = `/watch/${id}`;
  const channelHref = channelId ? `/channel/${channelId}` : undefined;

  const [previewing, setPreviewing] = useState(false);
  const [previewPlaying, setPreviewPlaying] = useState(false);
  const [previewTime, setPreviewTime] = useState(0);
  const [muted, setMuted] = useState(true);
  const [saveOpen, setSaveOpen] = useState(false);
  const [shareOpen, setShareOpen] = useState(false);
  const hoverTimer = useRef<number | undefined>(undefined);
  const videoRef = useRef<HTMLVideoElement | null>(null);

  useEffect(() => () => window.clearTimeout(hoverTimer.current), []);

  const startHover = () => {
    if (!videoUrl) return;
    window.clearTimeout(hoverTimer.current);
    hoverTimer.current = window.setTimeout(
      () => setPreviewing(true),
      PREVIEW_DELAY_MS,
    );
  };

  const endHover = () => {
    window.clearTimeout(hoverTimer.current);
    setPreviewing(false);
    setPreviewPlaying(false);
    setPreviewTime(0);
  };

  // Điểm tag cho gợi ý; lịch sử xem do trình phát ghi khi thực sự xem
  const recordView = () => {
    if (user?.sub) {
      saveInteraction({ userId: user.sub, videoId: id, type: "VIEW" })
        .unwrap()
        .catch((err) => console.error("Ghi nhận VIEW thất bại:", err));
    }
  };

  const toggleMute = (e: React.MouseEvent) => {
    e.preventDefault();
    e.stopPropagation();
    const v = videoRef.current;
    if (!v) return;
    v.muted = !v.muted;
    setMuted(v.muted);
  };

  const addToWatchLater = async () => {
    if (!user?.sub) {
      router.push("/login");
      return;
    }
    try {
      await playlistService.addToSystemPlaylist(user.sub, id, "WATCH_LATER");
      toast.success("Đã lưu vào Xem sau");
    } catch {
      toast.error("Không lưu được, vui lòng thử lại");
    }
  };

  const shownProgress =
    previewPlaying && duration > 0
      ? previewTime / duration
      : duration > 0
        ? watched / duration
        : 0;
  const shownTime = previewPlaying
    ? Math.max(0, duration - previewTime)
    : duration;

  return (
    <div
      className="group relative w-full"
      onMouseEnter={startHover}
      onMouseLeave={endHover}
    >
      {/* Nền nhạt quanh thẻ khi đang xem trước, giống YouTube */}
      <div
        className={cn(
          "pointer-events-none absolute -inset-2 rounded-2xl bg-gray-100 transition-opacity duration-200",
          previewing ? "opacity-100" : "opacity-0",
        )}
      />

      <div className="relative">
        <Link
          href={watchHref}
          onClick={recordView}
          className="block"
          aria-label={title}
        >
          <div className="relative aspect-video w-full overflow-hidden rounded-xl bg-gray-200">
            <Image
              src={thumbnailUrl || THUMBNAIL_FALLBACK}
              alt={title}
              fill
              sizes="(max-width: 640px) 100vw, (max-width: 1024px) 50vw, 33vw"
              className="object-cover"
            />

            {previewing && videoUrl && (
              <video
                ref={(el) => {
                  videoRef.current = el;
                  if (el) el.muted = muted; // phải tắt tiếng trước khi phát để trình duyệt cho tự phát
                }}
                src={videoUrl}
                playsInline
                preload="auto"
                onLoadedMetadata={(e) => {
                  const v = e.currentTarget;
                  // Xem dở thì xem trước tiếp từ chỗ đó
                  if (watched > 0 && watched < v.duration - 5)
                    v.currentTime = watched;
                  v.play().catch(() => {});
                }}
                onPlaying={() => setPreviewPlaying(true)}
                onTimeUpdate={(e) =>
                  setPreviewTime(e.currentTarget.currentTime)
                }
                className={cn(
                  "absolute inset-0 h-full w-full bg-black object-cover transition-opacity duration-300",
                  previewPlaying ? "opacity-100" : "opacity-0",
                )}
              />
            )}

            <span className="absolute right-2 bottom-2 rounded bg-black/80 px-1 py-0.5 text-xs font-medium text-white tabular-nums">
              {formatTime(shownTime)}
            </span>

            {shownProgress > 0 && (
              <div className="absolute inset-x-0 bottom-0 h-1 bg-gray-400/60">
                <div
                  className="h-full bg-red-600"
                  style={{ width: `${Math.min(100, shownProgress * 100)}%` }}
                />
              </div>
            )}
          </div>
        </Link>

        {/* Nằm ngoài link: nút bấm không được lồng trong thẻ <a> */}
        {previewPlaying && (
          <button
            type="button"
            onClick={toggleMute}
            aria-label={muted ? "Bật tiếng" : "Tắt tiếng"}
            className="absolute top-2 right-2 flex h-9 w-9 items-center justify-center rounded-full bg-black/60 text-white hover:bg-black/80"
          >
            {muted ? <VolumeX size={18} /> : <Volume2 size={18} />}
          </button>
        )}
      </div>

      <div className="relative mt-3 flex items-start gap-3">
        {channelHref ? (
          <Link
            href={channelHref}
            className="shrink-0"
            aria-label={`Kênh ${name}`}
          >
            <Image
              src={picture}
              alt={name}
              width={36}
              height={36}
              className="h-9 w-9 rounded-full object-cover"
            />
          </Link>
        ) : (
          <Image
            src={picture}
            alt={name}
            width={36}
            height={36}
            className="h-9 w-9 shrink-0 rounded-full object-cover"
          />
        )}

        <div className="min-w-0 flex-1">
          <Link
            href={watchHref}
            onClick={recordView}
            title={title}
            className="line-clamp-2 text-base leading-snug font-semibold text-black"
          >
            {title}
          </Link>
          {channelHref ? (
            <Link
              href={channelHref}
              className="mt-1 block truncate text-sm text-gray-600 hover:text-black"
            >
              {name}
            </Link>
          ) : (
            <p className="mt-1 truncate text-sm text-gray-600">{name}</p>
          )}
          <p className="text-sm text-gray-600">
            {formatViews(totalView)} • {timeAgo(createAt)}
          </p>
        </div>

        <DropdownMenu>
          <DropdownMenuTrigger asChild>
            <button
              type="button"
              aria-label="Tác vụ khác"
              className="-mr-2 flex h-9 w-9 shrink-0 items-center justify-center rounded-full hover:bg-gray-200 md:opacity-0 md:group-hover:opacity-100 md:focus-visible:opacity-100 md:data-[state=open]:opacity-100"
            >
              <MoreVertical size={20} />
            </button>
          </DropdownMenuTrigger>
          <DropdownMenuContent align="end" className="w-60">
            <DropdownMenuItem
              onSelect={addToWatchLater}
              className="cursor-pointer gap-3 py-2.5"
            >
              <Clock className="size-5" /> Lưu vào Xem sau
            </DropdownMenuItem>
            <DropdownMenuItem
              onSelect={() =>
                user?.sub ? setSaveOpen(true) : router.push("/login")
              }
              className="cursor-pointer gap-3 py-2.5"
            >
              <ListPlus className="size-5" /> Lưu vào danh sách phát
            </DropdownMenuItem>
            <DropdownMenuItem
              onSelect={() => setShareOpen(true)}
              className="cursor-pointer gap-3 py-2.5"
            >
              <Share2 className="size-5" /> Chia sẻ
            </DropdownMenuItem>
          </DropdownMenuContent>
        </DropdownMenu>
      </div>

      {saveOpen && (
        <SaveToPlaylistDialog
          videoId={id}
          open={saveOpen}
          onOpenChange={setSaveOpen}
        />
      )}
      {shareOpen && (
        <ShareDialog
          videoId={id}
          title={title}
          currentTime={0}
          open={shareOpen}
          onOpenChange={setShareOpen}
        />
      )}
    </div>
  );
};

export const VideoCardSkeleton = () => {
  return (
    <div className="w-full flex flex-col">
      <Skeleton className="w-full aspect-video rounded-xl" />
      <div className="flex gap-3 pt-3">
        <Skeleton className="w-9 h-9 rounded-full shrink-0" />
        <div className="flex-1 space-y-2">
          <Skeleton className="h-5 w-full" />
          <Skeleton className="h-5 w-3/4" />
          <Skeleton className="h-4 w-1/2" />
        </div>
      </div>
    </div>
  );
};

export default VideoCard;
