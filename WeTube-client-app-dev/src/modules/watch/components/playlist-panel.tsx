"use client";

import { useEffect, useRef, useState } from "react";
import Link from "next/link";
import { ChevronDown, ChevronUp, Repeat, Shuffle } from "lucide-react";
import { useAuth } from "@/contexts/auth-context";
import { useProgressRouter } from "@/hooks/use-progress-router";
import { toastEmitter } from "@/lib/toastEmitter";
import { cn } from "@/lib/utils";
import { canRemoveFromPlaylist, playlistDisplayTitle } from "@/modules/playlists/types";
import { playlistService } from "@/modules/playlists/ui/list/playlist-API";
import { PlaylistVideoRow } from "@/modules/playlists/ui/components/playlist-video-row";
import { usePlaylistPlaybackStore } from "@/store/zustand/usePlaylistPlaybackStore";
import { usePlaylistQueue } from "../lib/use-playlist-queue";

/** Khung danh sách phát ở cột phải trang xem (khi có ?list=), như YouTube. */
export default function PlaylistPanel({ videoId }: { videoId: string }) {
  const queue = usePlaylistQueue(videoId);
  const order = usePlaylistPlaybackStore((s) => s.order);
  const loop = usePlaylistPlaybackStore((s) => s.loop);
  const shuffle = usePlaylistPlaybackStore((s) => s.shuffle);
  const setLoop = usePlaylistPlaybackStore((s) => s.setLoop);
  const setShuffle = usePlaylistPlaybackStore((s) => s.setShuffle);
  const removeFromStore = usePlaylistPlaybackStore((s) => s.removeVideo);
  const { user } = useAuth();
  const router = useProgressRouter();
  const [collapsed, setCollapsed] = useState(false);
  const activeRef = useRef<HTMLDivElement>(null);

  // Đổi video: cuộn hàng đang phát vào giữa khung (chỉ cuộn trong khung, không cuộn cả trang)
  useEffect(() => {
    const row = activeRef.current;
    const box = row?.parentElement;
    if (row && box) box.scrollTop = row.offsetTop - box.clientHeight / 2 + row.clientHeight / 2;
  }, [videoId, collapsed, queue.detail?.playlistId]);

  const detail = queue.detail;
  if (!detail) return null;

  const title = playlistDisplayTitle(detail.playlistType, detail.playlistTitle);
  const canRemove = user?.sub === detail.ownerId && canRemoveFromPlaylist(detail.playlistType);
  const videos = order
    .map((id) => detail.videos.find((v) => v.videoId === id))
    .filter((v): v is NonNullable<typeof v> => !!v);

  const toggleShuffle = () => {
    setShuffle(!shuffle, videoId);
    const params = new URLSearchParams(window.location.search);
    if (shuffle) params.delete("shuffle");
    else params.set("shuffle", "1");
    router.replace(`/watch/${videoId}?${params.toString()}`);
  };

  const removeVideo = async (id: string) => {
    try {
      await playlistService.removeVideo(detail.playlistId, id);
      removeFromStore(id);
      toastEmitter.success(`Đã xoá khỏi ${title}`);
    } catch (err) {
      toastEmitter.error((err as Error).message);
    }
  };

  const iconButton = (on: boolean) =>
    cn("rounded-full p-2 hover:bg-gray-200", on ? "text-black bg-gray-200" : "text-gray-500");

  return (
    <section className="overflow-hidden rounded-xl border border-gray-200">
      <div className="flex items-start gap-2 bg-gray-50 px-4 py-3">
        <div className="min-w-0 flex-1">
          <Link href={`/playlists/${detail.playlistId}`} className="line-clamp-1 text-lg font-bold">
            {title}
          </Link>
          <p className="text-xs text-gray-600">
            {detail.ownerName ? `${detail.ownerName} · ` : ""}
            {queue.position > 0 ? queue.position : "–"}/{queue.total}
          </p>
          {!collapsed && (
            <div className="mt-1 flex gap-1">
              <button
                type="button"
                aria-pressed={loop}
                title={loop ? "Tắt lặp lại danh sách" : "Lặp lại danh sách"}
                onClick={() => setLoop(!loop)}
                className={iconButton(loop)}
              >
                <Repeat className="size-5" />
              </button>
              <button
                type="button"
                aria-pressed={shuffle}
                title={shuffle ? "Tắt trộn bài" : "Trộn bài"}
                onClick={toggleShuffle}
                className={iconButton(shuffle)}
              >
                <Shuffle className="size-5" />
              </button>
            </div>
          )}
        </div>
        <button
          type="button"
          aria-label={collapsed ? "Mở danh sách" : "Thu gọn danh sách"}
          onClick={() => setCollapsed((c) => !c)}
          className="rounded-full p-2 hover:bg-gray-200"
        >
          {collapsed ? <ChevronDown className="size-5" /> : <ChevronUp className="size-5" />}
        </button>
      </div>
      {!collapsed && (
        <div className="relative max-h-[400px] overflow-y-auto py-1">
          {videos.map((video, index) => {
            const active = video.videoId === videoId;
            return (
              <div key={video.videoId} ref={active ? activeRef : undefined}>
                <PlaylistVideoRow
                  index={index}
                  video={video}
                  playlistTitle={title}
                  href={queue.hrefFor(video.videoId)}
                  active={active}
                  compact
                  onRemove={canRemove ? () => removeVideo(video.videoId) : undefined}
                />
              </div>
            );
          })}
        </div>
      )}
    </section>
  );
}
