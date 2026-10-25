"use client";

import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { useSearchParams } from "next/navigation";
import { MoreVertical, Pencil, Play, Shuffle, Trash2, X } from "lucide-react";
import { playlistService } from "./playlist-API";
import ColorThief from "colorthief";
import Image from 'next/image';
import { Skeleton } from "@/components/ui/skeleton";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { useAuth } from "@/contexts/auth-context";
import { useProgressRouter } from "@/hooks/use-progress-router";
import { toastEmitter } from "@/lib/toastEmitter";
import { cn } from "@/lib/utils";
import {
  PLAYLIST_SORT_LABELS,
  arrangeVideos,
  type PlaylistChip,
  type PlaylistSort,
} from "@/lib/playlistOrder";
import { usePlaylistPlaybackStore } from "@/store/zustand/usePlaylistPlaybackStore";
import {
  canRemoveFromPlaylist,
  isEditablePlaylist,
  isPrivatePlaylist,
  playlistDisplayTitle,
  type PlaylistDetail as PlaylistDetailData,
} from "../../types";
import { PlaylistVideoRow } from "../components/playlist-video-row";
import { EditPlaylistDialog } from "../components/edit-playlist-dialog";
import { DeletePlaylistDialog } from "../components/delete-playlist-dialog";

/** Khung chờ cùng bố cục với trang danh sách phát: thẻ bìa bên trái, các hàng video bên phải. */
export function PlaylistDetailSkeleton() {
  return (
    <div className="w-full flex flex-col md:flex-row bg-white">
      <div className="w-full md:w-96 p-6">
        <Skeleton className="rounded-lg h-96 w-full" />
      </div>
      <div className="flex-1 space-y-4 p-6">
        {Array.from({ length: 5 }).map((_, i) => (
          <div key={i} className="flex gap-4 p-2">
            <div className="w-8 flex-shrink-0" />
            <Skeleton className="w-42 h-24 rounded-lg flex-shrink-0" />
            <div className="flex-1 space-y-2">
              <Skeleton className="h-5 w-3/4" />
              <Skeleton className="h-4 w-1/3" />
              <Skeleton className="h-4 w-1/4" />
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}

const CHIPS: { value: PlaylistChip; label: string }[] = [
  { value: "all", label: "Tất cả" },
  { value: "video", label: "Video" },
  { value: "short", label: "Shorts" },
];

const watchHref = (videoId: string, playlistId: string, shuffle = false) =>
  `/watch/${videoId}?list=${playlistId}${shuffle ? "&shuffle=1" : ""}`;

export default function PlaylistDetail({ playlistId }: { playlistId: string }) {
  const [playlist, setPlaylist] = useState<PlaylistDetailData | null>(null);
  const [loading, setLoading] = useState(true);
  const [bgColor, setBgColor] = useState<string>("rgb(107,114,128)");
  const [sort, setSort] = useState<PlaylistSort>("added-desc");
  const [chip, setChip] = useState<PlaylistChip>("all");
  const [hiddenNoticeClosed, setHiddenNoticeClosed] = useState(false);
  const [editOpen, setEditOpen] = useState(false);
  const [deleteOpen, setDeleteOpen] = useState(false);
  const imgRef = useRef<HTMLImageElement>(null);
  const { user } = useAuth();
  const router = useProgressRouter();
  const searchParams = useSearchParams();
  const startPlayback = usePlaylistPlaybackStore((s) => s.start);

  const fetchData = useCallback(async () => {
    if (!playlistId) return;
    try {
      const data = await playlistService.getDetail(playlistId);
      setPlaylist(data);
    } catch (err) {
      console.error("Error loading playlist detail", err);
    } finally {
      setLoading(false);
    }
  }, [playlistId]);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  useEffect(() => {
    if (!playlist?.videos?.length) return;
    if (imgRef.current) {
      const colorThief = new ColorThief();
      imgRef.current.crossOrigin = "anonymous";
      imgRef.current.onload = () => {
        try {
          const color = colorThief.getColor(imgRef.current!);
          setBgColor(`rgb(${color[0]}, ${color[1]}, ${color[2]})`);
        } catch (e) {
          console.error("ColorThief error:", e);
        }
      };
    }
  }, [playlist]);

  const shown = useMemo(
    () => (playlist ? arrangeVideos(playlist.videos, sort, chip) : []),
    [playlist, sort, chip]
  );
  const shownIds = useMemo(() => shown.map((v) => v.videoId), [shown]);

  // Thẻ danh sách "Phát tất cả" dẫn tới đây với ?play=1: phát video đầu theo thứ tự mặc định
  const autoPlayed = useRef(false);
  useEffect(() => {
    if (!playlist || autoPlayed.current || searchParams.get("play") !== "1" || shownIds.length === 0) return;
    autoPlayed.current = true;
    startPlayback(playlist, shownIds, false);
    router.replace(watchHref(shownIds[0], playlist.playlistId));
  }, [playlist, searchParams, shownIds, startPlayback, router]);

  if (loading) return <PlaylistDetailSkeleton />;
  if (!playlist) return <p className="p-6 text-gray-500">Không tìm thấy danh sách phát.</p>;

  const title = playlistDisplayTitle(playlist.playlistType, playlist.playlistTitle);
  const isOwner = !!user?.sub && user.sub === playlist.ownerId;
  const canEdit = isOwner && isEditablePlaylist(playlist.playlistType);
  const canRemove = isOwner && canRemoveFromPlaylist(playlist.playlistType);
  const cover = playlist.videos[0];

  const playAll = () => {
    if (!shownIds.length) return;
    startPlayback(playlist, shownIds, false);
    router.push(watchHref(shownIds[0], playlist.playlistId));
  };

  const playShuffled = () => {
    if (!shownIds.length) return;
    startPlayback(playlist, shownIds, true);
    const first = usePlaylistPlaybackStore.getState().order[0];
    router.push(watchHref(first, playlist.playlistId, true));
  };

  const removeVideo = async (videoId: string) => {
    try {
      await playlistService.removeVideo(playlist.playlistId, videoId);
      setPlaylist((p) =>
        p ? { ...p, videos: p.videos.filter((v) => v.videoId !== videoId), totalVideos: p.totalVideos - 1 } : p
      );
      const store = usePlaylistPlaybackStore.getState();
      if (store.detail?.playlistId === playlist.playlistId) store.removeVideo(videoId);
      toastEmitter.success(`Đã xoá khỏi ${title}`);
    } catch (err) {
      toastEmitter.error((err as Error).message);
    }
  };

  return (
    <div className="w-full flex flex-col md:flex-row bg-white">
      <div className="w-full md:w-96 md:sticky md:top-0 md:self-start p-6">
        <div
          className="rounded-2xl p-6 text-white flex flex-col"
          style={{ background: `linear-gradient(180deg, ${bgColor} 0%, rgba(15, 15, 15, 0.92) 100%)` }}
        >
          <Image
            ref={imgRef}
            src={cover?.thumbnailUrl || "/images/thumbnail.png"}
            alt={title}
            width={300}
            height={200}
            priority
            className="w-full aspect-video object-cover rounded-xl mb-4"
          />
          <div className="flex items-start gap-2">
            <h1 className="flex-1 text-2xl font-bold mb-2 break-words">{title}</h1>
            {canEdit && (
              <DropdownMenu>
                <DropdownMenuTrigger aria-label="Tác vụ khác" className="rounded-full p-2 bg-white/10 hover:bg-white/20">
                  <MoreVertical className="size-5" />
                </DropdownMenuTrigger>
                <DropdownMenuContent align="end">
                  <DropdownMenuItem onSelect={() => setDeleteOpen(true)} className="cursor-pointer gap-3 py-2.5">
                    <Trash2 className="size-5" /> Xoá
                  </DropdownMenuItem>
                  <DropdownMenuItem onSelect={() => setEditOpen(true)} className="cursor-pointer gap-3 py-2.5">
                    <Pencil className="size-5" /> Chỉnh sửa
                  </DropdownMenuItem>
                </DropdownMenuContent>
              </DropdownMenu>
            )}
          </div>
          {playlist.ownerName && <p className="font-semibold mb-1">{playlist.ownerName}</p>}
          <p className="text-sm text-white/80 mb-5">
            {playlist.totalVideos} video
            {isPrivatePlaylist(playlist.privacy) && " · Riêng tư"} · Cập nhật lần cuối vào{" "}
            {new Date(playlist.lastUpdatedAt || playlist.createdAt).toLocaleDateString("vi-VN")}
          </p>

          <div className="flex gap-2">
            <button
              type="button"
              onClick={playAll}
              disabled={!shownIds.length}
              className="flex flex-1 items-center justify-center gap-2 bg-white text-black px-4 py-2 rounded-full font-medium hover:bg-gray-100 disabled:opacity-50"
            >
              <Play size={18} fill="currentColor" />
              Phát tất cả
            </button>
            <button
              type="button"
              onClick={playShuffled}
              disabled={!shownIds.length}
              className="flex flex-1 items-center justify-center gap-2 bg-white/15 px-4 py-2 rounded-full font-medium hover:bg-white/25 disabled:opacity-50"
            >
              <Shuffle size={18} />
              Trộn bài
            </button>
          </div>
        </div>
      </div>

      <div className="flex-1 min-w-0 p-6">
        {playlist.hiddenCount > 0 && !hiddenNoticeClosed && (
          <div className="mb-4 flex items-center justify-between rounded-lg bg-gray-100 px-4 py-3 text-sm">
            <span>Đã ẩn {playlist.hiddenCount} video không xem được</span>
            <button type="button" aria-label="Đóng" onClick={() => setHiddenNoticeClosed(true)}>
              <X className="size-5" />
            </button>
          </div>
        )}

        <div className="mb-4 flex flex-wrap items-center gap-2">
          <Select value={sort} onValueChange={(v) => v && setSort(v as PlaylistSort)}>
            <SelectTrigger className="w-auto rounded-lg bg-gray-100 border-none font-medium">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              {(Object.keys(PLAYLIST_SORT_LABELS) as PlaylistSort[]).map((key) => (
                <SelectItem key={key} value={key}>
                  {PLAYLIST_SORT_LABELS[key]}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
          {CHIPS.map((c) => (
            <button
              key={c.value}
              type="button"
              onClick={() => setChip(c.value)}
              className={cn(
                "rounded-lg px-3 py-1.5 text-sm font-medium",
                chip === c.value ? "bg-black text-white" : "bg-gray-100 hover:bg-gray-200"
              )}
            >
              {c.label}
            </button>
          ))}
        </div>

        {shown.length === 0 ? (
          <p className="py-10 text-center text-gray-500">
            {playlist.videos.length === 0 ? "Danh sách phát này chưa có video nào." : "Không có video nào khớp."}
          </p>
        ) : (
          // Bấm một hàng: phát từ video đó theo đúng thứ tự đang hiển thị
          <div className="space-y-1" onClickCapture={() => startPlayback(playlist, shownIds, false)}>
            {shown.map((video, index) => (
              <PlaylistVideoRow
                key={video.videoId}
                index={index}
                video={video}
                playlistTitle={title}
                href={watchHref(video.videoId, playlist.playlistId)}
                onRemove={canRemove ? () => removeVideo(video.videoId) : undefined}
              />
            ))}
          </div>
        )}
      </div>

      {canEdit && (
        <>
          <EditPlaylistDialog playlist={playlist} open={editOpen} onOpenChange={setEditOpen} onSaved={fetchData} />
          <DeletePlaylistDialog
            playlist={playlist}
            open={deleteOpen}
            onOpenChange={setDeleteOpen}
            onDeleted={() => router.push("/playlists/allplaylists")}
          />
        </>
      )}
    </div>
  );
}
