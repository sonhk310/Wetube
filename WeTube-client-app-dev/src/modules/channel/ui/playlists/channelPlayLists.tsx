// modules/channel/ui/playlists/channelPlayLists.tsx
"use client";

import { useEffect, useState } from "react";
import { useParams } from "next/navigation";
import { useGetChannelInfoQuery } from "@/app/api/channelApi";
import { playlistService } from "@/modules/playlists/ui/list/playlist-API";
import PlaylistCard, { PlaylistItem } from "@/modules/channel/ui/playlists/playListsCard";

const FALLBACK_THUMB = "https://placehold.co/600x340?text=Playlist";

/** Playlist công khai do chủ kênh tạo (GET /api/playlists/created/{userId}). */
export default function ChannelPlaylists() {
  const { channelId } = useParams<{ channelId: string }>();
  const { data: channel } = useGetChannelInfoQuery(channelId);
  const [playlists, setPlaylists] = useState<PlaylistItem[] | null>(null);
  const [error, setError] = useState(false);

  useEffect(() => {
    if (!channel?.userId) return;
    playlistService
      .getCreated(channel.userId)
      .then((list: { playlistId: string; playlistTitle: string; totalVideos: number; thumbnailUrl: string | null }[]) =>
        setPlaylists(
          list.map((p) => ({
            id: p.playlistId,
            title: p.playlistTitle,
            videoCount: p.totalVideos,
            stackThumbnails: [p.thumbnailUrl || FALLBACK_THUMB],
          }))
        )
      )
      .catch(() => setError(true));
  }, [channel?.userId]);

  if (error) return <p className="p-4 text-red-500">Không tải được danh sách phát.</p>;
  if (playlists === null) return <p className="p-4 text-gray-500">Đang tải...</p>;
  if (playlists.length === 0) {
    return <p className="p-4 text-center text-gray-500">Kênh này chưa có danh sách phát công khai.</p>;
  }

  return (
    <div className="grid gap-6 p-4 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4">
      {playlists.map((playlist) => (
        <PlaylistCard key={playlist.id} playlist={playlist} />
      ))}
    </div>
  );
}
