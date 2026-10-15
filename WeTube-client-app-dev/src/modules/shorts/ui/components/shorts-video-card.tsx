"use client";
import { useEffect, useRef, useState } from "react";
import {
  Volume2,
  VolumeX,
  ThumbsUp,
  ThumbsDown,
  MessageCircle,
  Share2,
  X,
  Maximize2,
} from "lucide-react";

interface ShortsVideoCardProps {
  videoUrl: string;
  caption: string;
  username: string;
  avatar: string;
  hashtags: string;
  music: string;
  onReachEnd?: () => void;
}

export default function ShortsVideoCard({
  videoUrl,
  caption,
  username,
  avatar,
  hashtags,
  music,
  onReachEnd,
}: ShortsVideoCardProps) {
  const videoRef = useRef<HTMLVideoElement>(null);
  const containerRef = useRef<HTMLDivElement>(null);
  // const observerRef = useRef<IntersectionObserver | null>(null);

  const [showComment, setShowComment] = useState(false);
  const [commentInput, setCommentInput] = useState("");
  const [muted, setMuted] = useState(true);
  const [paused, setPaused] = useState(false);
  const [comments, setComments] = useState<string[]>(
    Array.from({ length: 10 }, (_, i) => `Đây là bình luận số ${i + 1}`)
  );

  const handleTogglePlay = () => {
    if (videoRef.current) {
      if (videoRef.current.paused) {
        videoRef.current.play();
        setPaused(false);
      } else {
        videoRef.current.pause();
        setPaused(true);
      }
    }
  };

  const handleToggleComment = () => setShowComment(!showComment);

  const handleSubmitComment = (e: React.FormEvent) => {
    e.preventDefault();
    if (commentInput.trim() !== "") {
      setComments((prev) => [...prev, commentInput.trim()]);
      setCommentInput("");
    }
  };

  const handleToggleFullscreen = () => {
    const el = containerRef.current;
    if (!el) return;

    const requestFullscreen =
      el.requestFullscreen ||
      (
        el as HTMLElement & {
          webkitRequestFullscreen?: () => Promise<void>;
          mozRequestFullScreen?: () => Promise<void>;
          msRequestFullscreen?: () => Promise<void>;
        }
      ).webkitRequestFullscreen ||
      (
        el as HTMLElement & {
          mozRequestFullScreen?: () => Promise<void>;
        }
      ).mozRequestFullScreen ||
      (
        el as HTMLElement & {
          msRequestFullscreen?: () => Promise<void>;
        }
      ).msRequestFullscreen;

    if (requestFullscreen) {
      requestFullscreen.call(el);
    }
  };

  useEffect(() => {
    const currentContainer = containerRef.current;
    if (!currentContainer) return;

    const observer = new IntersectionObserver(
      ([entry]) => {
        if (videoRef.current) {
          const isVisible =
            entry.isIntersecting && entry.intersectionRatio > 0.6;

          if (!showComment) {
            if (isVisible) {
              videoRef.current.play();
              setPaused(false);
              onReachEnd?.();
            } else {
              videoRef.current.pause();
              setPaused(true);
            }
          }
        }
      },
      { threshold: 0.6 }
    );

    observer.observe(currentContainer);

    return () => {
      if (currentContainer) {
        observer.unobserve(currentContainer);
      }
    };
  }, [onReachEnd, showComment]);

  return (
    <div
      ref={containerRef}
      className="relative bg-white flex justify-center items-center overflow-hidden snap-start"
    >
      <div
        className={`flex items-center gap-6 transition-transform duration-500 ease-in-out ${
          showComment ? "-translate-x-60" : ""
        }`}
      >
        <div
          className="relative aspect-[9/14] w-[360px] max-w-[90vw] sm:rounded-xl overflow-hidden bg-white"
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

          <div className="absolute top-4 left-4 right-4 sm:right-20 text-white text-sm font-semibold bg-black/60 px-3 py-1 rounded-md">
            {caption}
          </div>

          <div className="absolute bottom-4 left-4 right-20 text-white text-sm space-y-2">
            <div className="flex items-center gap-3">
              <Image
                src={avatar}
                alt="avatar"
                width={40}
                height={40}
                className="w-10 h-10 rounded-full object-cover"
              />
              <div>
                <p className="font-semibold">{username}</p>
                <button className="text-xs text-blue-400">Đăng ký</button>
              </div>
            </div>
            <p className="text-sm">{hashtags}</p>
            <p className="text-xs text-gray-300">🎵 {music}</p>
          </div>
        </div>

        <div className="hidden sm:flex flex-col items-center justify-center gap-6 text-black text-sm">
            <ActionButton
              icon={Maximize2}
              label="Fullscreen"
              onClick={handleToggleFullscreen}
            />
          <ActionButton
            icon={muted ? VolumeX : Volume2}
            label={muted ? "Tắt tiếng" : "Có tiếng"}
            onClick={() => setMuted((prev) => !prev)}
          />
          <ActionButton icon={ThumbsUp} label="27N" />
          <ActionButton icon={ThumbsDown} label="Không" />
          <ActionButton
            icon={MessageCircle}
            label={comments.length.toString()}
            onClick={handleToggleComment}
          />
          <ActionButton icon={Share2} label="Chia sẻ" />
        </div>
      </div>

      <div
        className={`absolute top-0 right-10 rounded-xl h-full w-[450px] max-w-[90vw] bg-[#111] text-white px-4 py-4 transition-all duration-500 ease-in-out ${
          showComment
            ? "translate-x-0 opacity-100"
            : "translate-x-full opacity-0 pointer-events-none"
        }`}
      >
        <div className="flex justify-between items-center mb-4">
          <h2 className="font-bold text-lg">Bình luận {comments.length}</h2>
          <button onClick={handleToggleComment}>
            <X className="w-5 h-5" />
          </button>
        </div>

        <div className="space-y-4 overflow-y-auto max-h-[calc(100%-100px)] pr-2 pb-24">
          {comments.map((cmt, i) => (
            <div key={i}>
              <p className="font-semibold">@user{i + 1}</p>
              <p>{cmt}</p>
            </div>
          ))}
        </div>

        <form
          onSubmit={handleSubmitComment}
          className="absolute bottom-4 left-0 right-0 px-4"
        >
          <input
            type="text"
            value={commentInput}
            onChange={(e) => setCommentInput(e.target.value)}
            placeholder="Viết bình luận..."
            className="w-full px-4 py-2 rounded-full bg-[#222] text-white placeholder-gray-400 border border-gray-600 focus:outline-none"
          />
        </form>
      </div>
    </div>
  );
}

import type { LucideIcon } from "lucide-react";
import Image from 'next/image';

function ActionButton({
  icon: Icon,
  label,
  onClick,
}: {
  icon: LucideIcon;
  label: string;
  onClick?: () => void;
}) {
  return (
    <div
      onClick={onClick}
      className="flex flex-col items-center cursor-pointer hover:scale-110 active:scale-95 transition"
    >
      <Icon className="text-black w-6 h-6" />
      <span className="text-xs mt-1 text-black">{label}</span>
    </div>
  );
}
