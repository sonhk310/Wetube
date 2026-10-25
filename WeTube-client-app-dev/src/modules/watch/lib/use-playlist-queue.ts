"use client";

import { useCallback, useEffect } from "react";
import { useSearchParams } from "next/navigation";
import { playlistService } from "@/modules/playlists/ui/list/playlist-API";
import type { PlaylistDetail, PlaylistVideoItem } from "@/modules/playlists/types";
import { nextInOrder, prevInOrder } from "@/lib/playlistOrder";
import { usePlaylistPlaybackStore } from "@/store/zustand/usePlaylistPlaybackStore";

export interface PlaylistQueue {
  listId: string | null;
  /** null khi không có ?list= hoặc chưa tải / tải lỗi: trang xem chạy như bình thường. */
  detail: PlaylistDetail | null;
  /** Vị trí 1-based của video đang phát trong thứ tự phát; 0 nếu không có trong danh sách. */
  position: number;
  total: number;
  next: PlaylistVideoItem | undefined;
  prev: PlaylistVideoItem | undefined;
  hrefFor: (videoId: string) => string;
}

// Khung danh sách và trình phát cùng gọi hook này: chỉ tải mỗi danh sách một lần
let loadingListId: string | null = null;

/** Danh sách phát của trang xem (`/watch/{id}?list=…&shuffle=1`): tải nếu cần, video trước / sau. */
export function usePlaylistQueue(videoId: string): PlaylistQueue {
  const searchParams = useSearchParams();
  const listId = searchParams.get("list");
  const shuffleParam = searchParams.get("shuffle") === "1";
  const stored = usePlaylistPlaybackStore((s) => s.detail);
  const order = usePlaylistPlaybackStore((s) => s.order);
  const loop = usePlaylistPlaybackStore((s) => s.loop);
  const shuffle = usePlaylistPlaybackStore((s) => s.shuffle);
  const load = usePlaylistPlaybackStore((s) => s.load);

  useEffect(() => {
    if (!listId || stored?.playlistId === listId || loadingListId === listId) return;
    loadingListId = listId;
    playlistService
      .getDetail(listId)
      .then((detail: PlaylistDetail) => load(detail, shuffleParam))
      .catch((err: unknown) => console.error("Không tải được danh sách phát:", err))
      .finally(() => {
        if (loadingListId === listId) loadingListId = null;
      });
  }, [listId, stored?.playlistId, shuffleParam, load]);

  const detail = listId && stored?.playlistId === listId ? stored : null;

  const hrefFor = useCallback(
    (id: string) => (detail ? `/watch/${id}?list=${detail.playlistId}${shuffle ? "&shuffle=1" : ""}` : `/watch/${id}`),
    [detail, shuffle]
  );

  if (!detail) {
    return { listId, detail: null, position: 0, total: 0, next: undefined, prev: undefined, hrefFor };
  }
  const byId = (id: string | undefined) => (id ? detail.videos.find((v) => v.videoId === id) : undefined);
  return {
    listId,
    detail,
    position: order.indexOf(videoId) + 1,
    total: order.length,
    next: byId(nextInOrder(order, videoId, loop)),
    prev: byId(prevInOrder(order, videoId)),
    hrefFor,
  };
}
