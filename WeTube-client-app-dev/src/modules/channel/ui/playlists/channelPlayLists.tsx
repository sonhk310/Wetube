// modules/channel/ui/playlists/channelPlayLists.tsx
"use client";

import { useCallback, useEffect, useState } from "react";
import { useParams } from "next/navigation";
import { useGetChannelInfoQuery } from "@/app/api/channelApi";
import { useAuth } from "@/contexts/auth-context";
import { playlistService } from "@/modules/playlists/ui/list/playlist-API";
import { PlaylistTile, PlaylistTileData } from "@/modules/playlists/ui/components/playlist-tile";
import { Skeleton } from "@/components/ui/skeleton";

/** Playlist công khai do chủ kênh tạo (GET /api/playlists/created/{userId}). */
export default function ChannelPlaylists() {
  const { channelId } = useParams<{ channelId: string }>();
  const { data: channel } = useGetChannelInfoQuery(channelId);
  const { user } = useAuth();
  const [playlists, setPlaylists] = useState<PlaylistTileData[] | null>(null);
  const [error, setError] = useState(false);

  const load = useCallback(() => {
    if (!channel?.userId) return;
    playlistService
      .getCreated(channel.userId)
      .then(setPlaylists)
      .catch(() => setError(true));
  }, [channel?.userId]);

  useEffect(load, [load]);

  if (error) return <p className="p-4 text-red-500">Không tải được danh sách phát.</p>;
  if (playlists === null) {
    return (
      <div className="grid gap-6 p-4 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4">
        {Array.from({ length: 4 }).map((_, i) => (
          <div key={i} className="space-y-2">
            <Skeleton className="aspect-video w-full rounded-xl" />
            <Skeleton className="h-5 w-3/4" />
            <Skeleton className="h-4 w-1/2" />
          </div>
        ))}
      </div>
    );
  }
  if (playlists.length === 0) {
    return <p className="p-4 text-center text-gray-500">Kênh này chưa có danh sách phát công khai.</p>;
  }

  return (
    <div className="grid gap-6 p-4 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4">
      {playlists.map((playlist) => (
        <PlaylistTile
          key={playlist.playlistId}
          playlist={playlist}
          isOwner={!!user?.sub && user.sub === channel?.userId}
          onChanged={load}
        />
      ))}
    </div>
  );
}
