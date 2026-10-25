"use client";

import { useGetPlaylistDetailsQuery, useGetPlaylistsByUserIdQuery } from "@/app/api/playlistApi";
import { useAuth } from "@/contexts/auth-context";
import type { VideoFromPlaylist } from "@/types/playlistSummary";

/** Mọi video người dùng đã tải lên (danh sách phát hệ thống "Video đã tải lên"), kể cả bản nháp. */
export function useUploadedVideos() {
  const { user, isLoading: authLoading } = useAuth();
  const playlists = useGetPlaylistsByUserIdQuery(user?.sub ?? "", { skip: !user?.sub });
  const uploadedId = playlists.data?.find((p) => p.playlistType === "USER_UPLOADED")?.playlistId;
  const detail = useGetPlaylistDetailsQuery(uploadedId!, { skip: !uploadedId });

  const videos: VideoFromPlaylist[] = detail.data?.videos ?? [];
  return {
    videos,
    isLoading: authLoading || playlists.isLoading || detail.isLoading,
    error: playlists.error || detail.error,
  };
}
