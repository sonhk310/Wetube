"use client";
import { useEffect, useRef, useState } from "react";
import { useProgressRouter } from "@/hooks/use-progress-router";
import Image from "next/image";
import {
  Volume2,
  VolumeX,
  ThumbsUp,
  MessageCircle,
  Share2,
  Maximize2,
} from "lucide-react";
import type { LucideIcon } from "lucide-react";
import { useAuth } from "@/contexts/auth-context";
import { useGetLikeInfoQuery, useToggleLikeMutation } from "@/app/api/likeApi";
import { formatViews } from "@/lib/utils";
import type { ShortVideo } from "@/modules/shorts/hooks/useShortsFeed";
import { useViewCounter } from "@/lib/viewTracking";

interface ShortsVideoCardProps extends ShortVideo {
  onReachEnd?: () => void;
}

export default function ShortsVideoCard({
  id,
  videoUrl,
  title,
  totalView,
  tagsAsString,
  user: author,
  onReachEnd,
}: ShortsVideoCardProps) {
  const router = useProgressRouter();
  const { user } = useAuth();
  const videoRef = useRef<HTMLVideoElement>(null);
  const containerRef = useRef<HTMLDivElement>(null);
  const [muted, setMuted] = useState(true);
  const [paused, setPaused] = useState(false);
  const [copied, setCopied] = useState(false);
  const [visible, setVisible] = useState(false);

  // Short hiện trong khung nhìn đủ 3 giây thì tính 1 lượt xem
  useViewCounter(id, visible, 3000);

  const { data: likeInfo } = useGetLikeInfoQuery(
    { targetId: id, targetType: "VIDEO", userId: user?.sub ?? "" },
    { skip: !user?.sub }
  );
  const [toggleLike] = useToggleLikeMutation();

  const handleTogglePlay = () => {
    const v = videoRef.current;
    if (!v) return;
    if (v.paused) {
      v.play();
      setPaused(false);
    } else {
      v.pause();
      setPaused(true);
    }
  };

  const handleLike = async () => {
    if (!user?.sub) return;
    try {
      await toggleLike({ targetId: id, targetType: "VIDEO", userId: user.sub }).unwrap();
    } catch (err) {
      console.error("Like short failed:", err);
    }
  };

  const handleShare = async () => {
    try {
      await navigator.clipboard.writeText(`${window.location.origin}/watch/${id}`);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch (err) {
      console.error("Copy link failed:", err);
    }
  };

  const handleToggleFullscreen = () => {
    containerRef.current?.requestFullscreen?.();
  };

  useEffect(() => {
    const currentContainer = containerRef.current;
    if (!currentContainer) return;

    const observer = new IntersectionObserver(
      ([entry]) => {
        const v = videoRef.current;
        if (!v) return;
        if (entry.isIntersecting && entry.intersectionRatio > 0.6) {
          v.play().catch(() => setPaused(true));
          setPaused(false);
          setVisible(true);
          onReachEnd?.();
        } else {
          v.pause();
          setPaused(true);
          setVisible(false);
        }
      },
      { threshold: 0.6 }
    );

    observer.observe(currentContainer);
    return () => observer.unobserve(currentContainer);
  }, [onReachEnd]);

  return (
    <div
      ref={containerRef}
      className="relative bg-white flex justify-center items-center overflow-hidden snap-start h-full"
    >
      <div className="flex items-center gap-6">
        <div
          className="relative aspect-[9/14] w-[360px] max-w-[90vw] sm:rounded-xl overflow-hidden bg-black"
          onClick={handleTogglePlay}
        >
          <video
            ref={videoRef}
            src={videoUrl}
            className="w-full h-full object-cover cursor-pointer"
            loop
            muted={muted}
            playsInline
          />
          {paused && (
            <div className="absolute inset-0 flex items-center justify-center bg-black/30">
              <div className="text-white text-5xl">▶️</div>
            </div>
          )}

          <div className="absolute bottom-4 left-4 right-4 text-white text-sm space-y-2">
            <p className="font-semibold line-clamp-2">{title}</p>
            {author && (
              <div className="flex items-center gap-3">
                <Image
                  src={author.picture || "https://placehold.co/40x40?text=W"}
                  alt={author.name}
                  width={40}
                  height={40}
                  className="w-10 h-10 rounded-full object-cover"
                />
                <p className="font-semibold">{author.name}</p>
              </div>
            )}
            {tagsAsString && <p className="text-xs">{tagsAsString}</p>}
            <p className="text-xs text-gray-300">{formatViews(totalView)}</p>
          </div>
        </div>

        <div className="hidden sm:flex flex-col items-center justify-center gap-6 text-black text-sm">
          <ActionButton icon={Maximize2} label="Toàn màn hình" onClick={handleToggleFullscreen} />
          <ActionButton
            icon={muted ? VolumeX : Volume2}
            label={muted ? "Tắt tiếng" : "Có tiếng"}
            onClick={() => setMuted((prev) => !prev)}
          />
          <ActionButton
            icon={ThumbsUp}
            label={String(likeInfo?.likeCount ?? 0)}
            active={!!likeInfo?.liked}
            onClick={handleLike}
          />
          <ActionButton
            icon={MessageCircle}
            label="Bình luận"
            onClick={() => router.push(`/watch/${id}`)}
          />
          <ActionButton icon={Share2} label={copied ? "Đã sao chép" : "Chia sẻ"} onClick={handleShare} />
        </div>
      </div>
    </div>
  );
}

function ActionButton({
  icon: Icon,
  label,
  active,
  onClick,
}: {
  icon: LucideIcon;
  label: string;
  active?: boolean;
  onClick?: () => void;
}) {
  return (
    <div
      onClick={onClick}
      className="flex flex-col items-center cursor-pointer hover:scale-110 active:scale-95 transition"
    >
      <Icon className={`w-6 h-6 ${active ? "text-blue-600 fill-blue-600" : "text-black"}`} />
      <span className="text-xs mt-1">{label}</span>
    </div>
  );
}
