"use client";

import { Skeleton } from "@/components/ui/skeleton";
import React, { useEffect, useState } from "react";
import { useAuth } from "@/contexts/auth-context";
import {
  useGetPlaylistDetailsQuery,
  useGetPlaylistsByUserIdQuery,
} from "@/app/api/playlistApi";
import { VideoFromPlaylist } from "@/types/playlistSummary";
import { useDeleteVideoMutation } from "@/app/api/videoApi";
import { playlistApi } from "@/app/api/playlistApi";
import { useDispatch } from "react-redux";
import { toastEmitter } from "@/lib/toastEmitter";
import { ContentRow } from "../components/content-row";
import { DeleteVideoDialog } from "../components/delete-video-dialog";

export default function VideosSection() {
  const { user } = useAuth();
  const dispatch = useDispatch();
  const [deleteVideo, { isLoading: isDeleting }] = useDeleteVideoMutation();
  const [videoToDelete, setVideoToDelete] = useState<VideoFromPlaylist | null>(null);

  const handleConfirmDelete = async () => {
    if (!videoToDelete) return;
    try {
      await deleteVideo(videoToDelete.videoId).unwrap();
      dispatch(playlistApi.util.invalidateTags(["Playlist"]));
      toastEmitter.success(videoToDelete.videoStatus === "DRAFT" ? "Đã xoá video nháp" : "Đã xoá video");
      setVideoToDelete(null);
    } catch (err) {
      console.error("Delete video failed:", err);
      toastEmitter.error("Không xoá được video");
    }
  };

  const {
    data: allPlaylists,
    isLoading: isLoadingPlaylists,
    error: playlistsError,
  } = useGetPlaylistsByUserIdQuery(user?.sub ?? "", {
    skip: !user?.sub,
  });

  const uploadedPlaylistId = allPlaylists?.find(
    (p) => p.playlistType === "USER_UPLOADED"
  )?.playlistId;

  const {
    data: playlistDetail,
    isLoading: isLoadingDetails,
    error: detailsError,
  } = useGetPlaylistDetailsQuery(uploadedPlaylistId!, {
    skip: !uploadedPlaylistId,
  });

  const isLoading = isLoadingPlaylists || isLoadingDetails;
  const error = playlistsError || detailsError;
  const videosFromPlaylist: VideoFromPlaylist[] = playlistDetail?.videos || [];
  
  const sortedVideos = [...videosFromPlaylist].sort((a, b) => {
    const dateB = new Date(b.updatedAt || b.createdAt).getTime();
    const dateA = new Date(a.updatedAt || a.createdAt).getTime();
    return dateB - dateA;
  });

  useEffect(() => {
    if (allPlaylists) {
      console.log("✅ Playlists Fetched:", allPlaylists);
    }
    if (playlistDetail) {
      console.log("✅ Playlist Detail Fetched:", playlistDetail);
    }
    if (error) {
      console.error("❌ API Error:", error);
    }
  }, [allPlaylists, playlistDetail, error]);

  if (isLoading) {
    return <VideosSectionSkeleton />;
  }

  if (error) {
    return (
      <div className="p-6 text-center text-red-500">
        Không tải được danh sách video. Vui lòng thử lại sau.
      </div>
    );
  }

  if (videosFromPlaylist.length === 0) {
    return (
      <div className="p-6 text-center text-muted-foreground">
        Bạn chưa tải lên video nào.
      </div>
    );
  }

  const longVideos = sortedVideos.filter((v) => !v.isShort);
  const shorts = sortedVideos.filter((v) => v.isShort);

  return (
    <div className="border-t">
      <ContentRow
        title="Video"
        videos={longVideos}
        vertical={false}
        emptyText="Chưa có video nào."
        onDelete={setVideoToDelete}
      />
      <ContentRow
        title="Shorts"
        videos={shorts}
        vertical
        emptyText="Chưa có Shorts nào. Video dọc hoặc vuông dài tối đa 3 phút sẽ là Shorts."
        onDelete={setVideoToDelete}
      />

      <DeleteVideoDialog
        video={videoToDelete}
        deleting={isDeleting}
        onCancel={() => setVideoToDelete(null)}
        onConfirm={handleConfirmDelete}
      />
    </div>
  );
}

const VideosSectionSkeleton = () => {
  return (
    <div className="border-t">
      {[false, true].map((vertical) => (
        <div key={String(vertical)} className="px-6 py-4">
          <Skeleton className="mb-3 h-6 w-24" />
          <div className="flex gap-4 overflow-hidden">
            {Array.from({ length: 5 }).map((_, index) => (
              <div key={index} className={vertical ? "w-44 shrink-0" : "w-56 shrink-0"}>
                <Skeleton className={vertical ? "aspect-[9/16] w-full rounded-xl" : "aspect-video w-full rounded-xl"} />
                <Skeleton className="mt-2 h-4 w-3/4" />
              </div>
            ))}
          </div>
        </div>
      ))}
    </div>
  );
};
