"use client";
import { useCallback, useEffect, useRef, useState } from "react";
import Image from "next/image";
import Link from "next/link";
import { toast } from "sonner";
import { Pause, Play, Volume2, VolumeX, X } from "lucide-react";
import { useAuth } from "@/contexts/auth-context";
import { useIsMobile } from "@/hooks/use-mobile";
import { useGetLikeInfoQuery, useToggleLikeMutation } from "@/app/api/likeApi";
import { useGetVideoDetailQuery, useToggleDislikeMutation } from "@/app/api/videoApi";
import { useGetCommentsByTargetQuery } from "@/app/api/commentApi";
import { useSubscribeMutation, useUnsubscribeMutation } from "@/app/api/subscriptionsApi";
import { useVideoStore } from "@/store/zustand/videoStore";
import { useViewCounter } from "@/lib/viewTracking";
import { cn } from "@/lib/utils";
import { Sheet, SheetContent, SheetHeader, SheetTitle } from "@/components/ui/sheet";
import { Spinner } from "@/components/ui/spinner";
import { Comments } from "@/modules/watch/components/comments/comments";
import ShareDialog from "@/modules/watch/components/share-dialog";
import SaveToPlaylistDialog from "@/modules/watch/components/save-to-playlist-dialog";
import ReportDialog from "@/modules/reports/report-dialog";
import type { ShortVideo } from "@/modules/shorts/hooks/useShortsFeed";
import { useShortsVolume } from "@/modules/shorts/hooks/useShortsVolume";
import { ShortsActions } from "./shorts-actions";
import { ShortsProgress } from "./shorts-progress";

interface ShortsVideoCardProps extends ShortVideo {
  /** Short đang chiếm màn hình: chỉ Short này phát, tải chi tiết, nhận phím tắt. */
  active: boolean;
  onActive: () => void;
  onReachEnd?: () => void;
}

function CommentsLoading() {
  return (
    <div className="flex justify-center py-10">
      <Spinner />
    </div>
  );
}

export function isTypingTarget(target: EventTarget | null) {
  const el = target as HTMLElement | null;
  return !!el && (el.isContentEditable || ["INPUT", "TEXTAREA", "SELECT"].includes(el.tagName));
}

export default function ShortsVideoCard({
  id,
  videoUrl,
  title,
  user: author,
  active,
  onActive,
  onReachEnd,
}: ShortsVideoCardProps) {
  const { user } = useAuth();
  const isMobile = useIsMobile();
  const videoRef = useRef<HTMLVideoElement>(null);
  const containerRef = useRef<HTMLDivElement>(null);
  const { volume, muted, setVolume, setMuted } = useShortsVolume();
  // Trình duyệt chặn tự phát có tiếng (chưa có thao tác nào trên trang): phát không tiếng, không lưu lựa chọn
  const [autoMuted, setAutoMuted] = useState(false);
  const [paused, setPaused] = useState(true);
  const [flash, setFlash] = useState<{ icon: "play" | "pause"; id: number } | null>(null);
  const [commentsOpen, setCommentsOpen] = useState(false);
  const [shareOpen, setShareOpen] = useState(false);
  const [saveOpen, setSaveOpen] = useState(false);
  const [reportOpen, setReportOpen] = useState(false);
  const [subscribedOverride, setSubscribedOverride] = useState<boolean | null>(null);
  const [subscribing, setSubscribing] = useState(false);
  const setVideoDetail = useVideoStore((s) => s.setVideoDetail);
  // Store còn giữ Short trước cho tới khi chi tiết Short này tải xong: chưa hiện bình luận
  const storeHasThisShort = useVideoStore((s) => s.videoDetail?.detail.id === id);
  const effectiveMuted = muted || autoMuted;

  // Short hiện trên màn hình đủ 3 giây thì tính 1 lượt xem
  useViewCounter(id, active, 3000);

  // Chi tiết (kênh, đã đăng ký chưa) và bình luận chỉ tải cho Short đang xem
  const { data: detail } = useGetVideoDetailQuery({ videoId: id }, { skip: !active });
  const { data: comments } = useGetCommentsByTargetQuery(
    { targetId: id, targetType: "VIDEO", sort: "top" },
    { skip: !active }
  );
  const { data: likeInfo, refetch: refetchLike } = useGetLikeInfoQuery(
    { targetId: id, targetType: "VIDEO", userId: user?.sub ?? "" },
    { skip: !user?.sub || !active }
  );
  const [toggleLike] = useToggleLikeMutation();
  const [toggleDislike] = useToggleDislikeMutation();
  const [subscribe] = useSubscribeMutation();
  const [unsubscribe] = useUnsubscribeMutation();

  const channelId = detail?.detail.channelId ?? author?.id;
  const isOwnChannel = !!user?.channelId && user.channelId === channelId;
  const isSubscribed = subscribedOverride ?? !!detail?.detail.subscribed;
  const likeCount = likeInfo?.likeCount ?? detail?.detail.like?.likeCount ?? 0;
  // Như trang xem: số bình luận tính cả phản hồi
  const commentCount = comments?.reduce((sum, c) => sum + 1 + (c.replyCount ?? 0), 0);

  // Khung bình luận đọc chi tiết video từ store (dùng chung với trang xem)
  useEffect(() => {
    if (active && detail) setVideoDetail(detail);
  }, [active, detail, setVideoDetail]);

  useEffect(() => {
    setSubscribedOverride(null);
  }, [detail]);

  const onActiveRef = useRef(onActive);
  const onReachEndRef = useRef(onReachEnd);
  useEffect(() => {
    onActiveRef.current = onActive;
    onReachEndRef.current = onReachEnd;
  });

  useEffect(() => {
    const el = containerRef.current;
    if (!el) return;
    const observer = new IntersectionObserver(
      ([entry]) => {
        if (entry.isIntersecting && entry.intersectionRatio > 0.6) {
          onActiveRef.current();
          onReachEndRef.current?.();
        }
      },
      { threshold: 0.6 }
    );
    observer.observe(el);
    return () => observer.disconnect();
  }, []);

  // Vào màn hình: phát lại từ đầu như YouTube; ra khỏi màn hình: dừng, đóng bình luận
  useEffect(() => {
    const v = videoRef.current;
    if (!v) return;
    if (!active) {
      v.pause();
      setCommentsOpen(false);
      return;
    }
    const saved = useShortsVolume.getState();
    v.currentTime = 0;
    v.volume = saved.volume;
    v.muted = saved.muted;
    v.play().catch(() => {
      if (v.muted) return;
      v.muted = true;
      setAutoMuted(true);
      v.play().catch(() => {});
    });
  }, [active]);

  useEffect(() => {
    const v = videoRef.current;
    if (!v) return;
    v.volume = volume;
    v.muted = effectiveMuted;
  }, [volume, effectiveMuted]);

  useEffect(() => {
    if (!flash) return;
    const t = setTimeout(() => setFlash(null), 600);
    return () => clearTimeout(t);
  }, [flash]);

  const togglePlay = useCallback(() => {
    const v = videoRef.current;
    if (!v) return;
    if (v.paused) {
      v.play().catch(() => {});
      setFlash({ icon: "play", id: Date.now() });
    } else {
      v.pause();
      setFlash({ icon: "pause", id: Date.now() });
    }
  }, []);

  const toggleMute = useCallback(() => {
    if (effectiveMuted) {
      setAutoMuted(false);
      if (volume === 0) setVolume(1);
      else setMuted(false);
    } else {
      setMuted(true);
    }
  }, [effectiveMuted, volume, setVolume, setMuted]);

  // Phím tắt của Short đang xem: Space / K phát / dừng, M tắt tiếng (↑ / ↓ ở danh sách)
  useEffect(() => {
    if (!active) return;
    const onKey = (e: KeyboardEvent) => {
      if (isTypingTarget(e.target) || e.ctrlKey || e.metaKey || e.altKey) return;
      const key = e.key.toLowerCase();
      if (key === " " && (e.target as HTMLElement | null)?.tagName === "BUTTON") return;
      if (key === " " || key === "k") {
        e.preventDefault();
        togglePlay();
      } else if (key === "m") {
        toggleMute();
      }
    };
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [active, togglePlay, toggleMute]);

  const requireLogin = (message: string) => {
    if (user?.sub) return false;
    toast.error(message);
    return true;
  };

  const handleLike = async () => {
    if (requireLogin("Hãy đăng nhập để thích video")) return;
    try {
      await toggleLike({ targetId: id, targetType: "VIDEO", userId: user!.sub }).unwrap();
    } catch (err) {
      console.error("Like short failed:", err);
    }
  };

  const handleDislike = async () => {
    if (requireLogin("Hãy đăng nhập để đánh giá video")) return;
    try {
      await toggleDislike(id).unwrap();
      await refetchLike();
    } catch (err) {
      console.error("Dislike short failed:", err);
    }
  };

  const handleSubscribe = async () => {
    if (requireLogin("Hãy đăng nhập để đăng ký kênh") || !channelId || subscribing) return;
    setSubscribing(true);
    try {
      if (!isSubscribed) {
        await subscribe({ subscriberId: user!.sub, channelId, tierId: undefined }).unwrap();
        setSubscribedOverride(true);
        toast.success("Đã đăng ký kênh");
      } else {
        await unsubscribe({ subscriberId: user!.sub, channelId }).unwrap();
        setSubscribedOverride(false);
        toast.success("Đã huỷ đăng ký");
      }
    } catch (err) {
      console.error("Failed to update subscription:", err);
      toast.error("Không cập nhật được đăng ký, vui lòng thử lại");
    } finally {
      setSubscribing(false);
    }
  };

  const actionProps = {
    likeCount,
    liked: !!likeInfo?.liked,
    disliked: !!likeInfo?.disliked,
    commentCount,
    onLike: handleLike,
    onDislike: handleDislike,
    onComment: () => setCommentsOpen((o) => !o),
    onShare: () => setShareOpen(true),
    onSave: () => setSaveOpen(true),
    onReport: () => setReportOpen(true),
  };

  const iconButton = "flex size-10 items-center justify-center rounded-full hover:bg-white/20";

  return (
    <div ref={containerRef} className="flex h-full items-center justify-center gap-4 md:py-4">
      <div className="relative h-full w-full overflow-hidden bg-black md:w-auto md:aspect-[9/16] md:rounded-xl">
        <video
          ref={videoRef}
          src={videoUrl}
          className="absolute inset-0 size-full cursor-pointer object-contain"
          loop
          playsInline
          preload="metadata"
          onClick={togglePlay}
          onPlay={() => setPaused(false)}
          onPause={() => setPaused(true)}
        />

        {/* Góc trên: phát / dừng, âm lượng */}
        <div className="pointer-events-none absolute inset-x-0 top-0 z-10 flex items-center gap-1 bg-gradient-to-b from-black/60 to-transparent p-2 pb-8 text-white">
          <button
            type="button"
            aria-label={paused ? "Phát (k)" : "Tạm dừng (k)"}
            title={paused ? "Phát (k)" : "Tạm dừng (k)"}
            onClick={togglePlay}
            className={cn(iconButton, "pointer-events-auto")}
          >
            {paused ? <Play className="size-6" fill="currentColor" /> : <Pause className="size-6" fill="currentColor" />}
          </button>
          <div className="group/volume pointer-events-auto flex items-center">
            <button
              type="button"
              aria-label={effectiveMuted ? "Bật tiếng (m)" : "Tắt tiếng (m)"}
              title={effectiveMuted ? "Bật tiếng (m)" : "Tắt tiếng (m)"}
              onClick={toggleMute}
              className={iconButton}
            >
              {effectiveMuted || volume === 0 ? <VolumeX className="size-6" /> : <Volume2 className="size-6" />}
            </button>
            <input
              type="range"
              min={0}
              max={1}
              step={0.05}
              value={effectiveMuted ? 0 : volume}
              aria-label="Âm lượng"
              onChange={(e) => {
                setAutoMuted(false);
                setVolume(Number(e.target.value));
              }}
              className="h-1 w-0 cursor-pointer accent-white opacity-0 transition-all group-hover/volume:w-20 group-hover/volume:opacity-100 focus:w-20 focus:opacity-100"
            />
          </div>
        </div>

        {/* Biểu tượng to giữa video khi bấm phát / dừng */}
        {flash && (
          <div key={flash.id} className="pointer-events-none absolute inset-0 z-10 flex items-center justify-center">
            <span className="flex size-16 items-center justify-center rounded-full bg-black/50 text-white animate-out fade-out-0 zoom-out-50 duration-500 fill-mode-forwards">
              {flash.icon === "play" ? (
                <Play className="size-8" fill="currentColor" />
              ) : (
                <Pause className="size-8" fill="currentColor" />
              )}
            </span>
          </div>
        )}

        {/* Góc dưới: kênh, Đăng ký, tiêu đề */}
        <div className="pointer-events-none absolute inset-x-0 bottom-0 z-10 space-y-2 bg-gradient-to-t from-black/70 to-transparent p-3 pb-5 pr-20 pt-12 text-white md:pr-3">
          {author && (
            <div className="pointer-events-auto flex items-center gap-2">
              <Link
                href={channelId ? `/channel/${channelId}` : "#"}
                className="flex min-w-0 items-center gap-2"
              >
                <Image
                  src={author.picture || "https://placehold.co/40x40?text=W"}
                  alt={author.name}
                  width={32}
                  height={32}
                  className="size-8 shrink-0 rounded-full object-cover"
                />
                <span className="truncate text-sm font-semibold">{author.name}</span>
              </Link>
              {channelId && !isOwnChannel && (
                <button
                  type="button"
                  onClick={handleSubscribe}
                  disabled={subscribing}
                  className={cn(
                    "shrink-0 rounded-full px-3 py-1.5 text-sm font-medium disabled:opacity-70",
                    isSubscribed ? "bg-white/20 text-white hover:bg-white/30" : "bg-white text-black hover:bg-gray-200"
                  )}
                >
                  {isSubscribed ? "Đã đăng ký" : "Đăng ký"}
                </button>
              )}
            </div>
          )}
          <p className="line-clamp-2 text-sm">{title}</p>
        </div>

        {/* Điện thoại: cột nút nằm trên video */}
        <ShortsActions variant="overlay" className="absolute bottom-20 right-2 z-20 md:hidden" {...actionProps} />

        <ShortsProgress videoRef={videoRef} active={active} />
      </div>

      <ShortsActions variant="side" className="hidden self-end pb-2 md:flex" {...actionProps} />

      {/* Máy tính: khung bình luận cạnh video */}
      {commentsOpen && !isMobile && (
        <aside className="relative hidden h-full w-[400px] shrink-0 flex-col overflow-hidden rounded-xl border bg-white md:flex">
          <button
            type="button"
            aria-label="Đóng bình luận"
            onClick={() => setCommentsOpen(false)}
            className="absolute right-3 top-3 z-10 flex size-9 items-center justify-center rounded-full hover:bg-gray-100"
          >
            <X className="size-5" />
          </button>
          <div className="flex-1 overflow-y-auto px-4 pb-4">
            {storeHasThisShort ? <Comments /> : <CommentsLoading />}
          </div>
        </aside>
      )}

      {/* Điện thoại: bình luận trượt từ dưới lên */}
      {isMobile && (
        <Sheet open={commentsOpen} onOpenChange={setCommentsOpen}>
          <SheetContent side="bottom" className="h-[75vh] gap-0 rounded-t-2xl">
            <SheetHeader className="pb-0">
              <SheetTitle>Bình luận</SheetTitle>
            </SheetHeader>
            <div className="flex-1 overflow-y-auto px-4 pb-4">
              {storeHasThisShort ? <Comments /> : <CommentsLoading />}
            </div>
          </SheetContent>
        </Sheet>
      )}

      <ShareDialog videoId={id} title={title} currentTime={0} open={shareOpen} onOpenChange={setShareOpen} />
      <SaveToPlaylistDialog videoId={id} open={saveOpen} onOpenChange={setSaveOpen} />
      <ReportDialog open={reportOpen} onOpenChange={setReportOpen} target={{ videoId: id }} title="Báo cáo video" />
    </div>
  );
}
