"use client";

import { useEffect, useState } from "react";
import { useAuth } from "@/contexts/auth-context";
import PlaylistDetail, { PlaylistDetailSkeleton } from "../list/playlistdetail-list";
import { playlistService } from "../list/playlist-API";

export default function PlaylistWatch() {
  const { user, isLoading: authLoading } = useAuth();
  const [playlistId, setPlaylistId] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!user?.sub) {
      // Chờ phiên đăng nhập xong, nếu không sẽ nháy "Không có playlist" trước khi tải
      if (!authLoading) setLoading(false);
      return;
    }

    const fetchWatchLater = async () => {
      try {
        const data = await playlistService.getByUserAndType(
          user.sub,
          "WATCH_LATER"
        );

        if (Array.isArray(data) && data.length > 0) {
          setPlaylistId(data[0].playlistId);
        } else {
          console.warn("Không tìm thấy Watch Later playlist");
        }
      } catch (err) {
        console.error("Lỗi fetch Watch Later:", err);
      } finally {
        setLoading(false);
      }
    };

    fetchWatchLater();
  }, [user?.sub, authLoading]);

  if (loading) return <PlaylistDetailSkeleton />;
  if (!playlistId) return <p className="p-6 text-gray-500">Chưa có video nào trong danh sách Xem sau.</p>;

  return <PlaylistDetail playlistId={playlistId} />;
}
