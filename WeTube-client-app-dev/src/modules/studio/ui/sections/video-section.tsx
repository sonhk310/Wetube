"use client";

import { Skeleton } from "@/components/ui/skeleton";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { VideoThumbnail } from "@/modules/videos/ui/components/video-thumbnail";
import Link from "next/link";
import React, { useEffect, useState } from "react";
import { useAuth } from "@/contexts/auth-context";
import {
  useGetPlaylistDetailsQuery,
  useGetPlaylistsByUserIdQuery,
} from "@/app/api/playlistApi";
import { VideoFromPlaylist } from "@/types/playlistSummary";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { useDeleteVideoMutation } from "@/app/api/videoApi";
import { playlistApi } from "@/app/api/playlistApi";
import { useDispatch } from "react-redux";
import toast from "react-hot-toast";

const STATUS_LABELS: Record<string, { label: string; className: string }> = {
  ACTIVE: { label: "Công khai", className: "bg-green-100 text-green-700" },
  PUBLIC: { label: "Công khai", className: "bg-green-100 text-green-700" },
  UNLISTED: { label: "Không công khai", className: "bg-yellow-100 text-yellow-700" },
  PRIVATE: { label: "Riêng tư", className: "bg-gray-200 text-gray-700" },
  INACTIVE: { label: "Bị ẩn", className: "bg-red-100 text-red-700" },
  DRAFT: { label: "Bản nháp", className: "bg-blue-100 text-blue-700" },
};

const formatDate = (dateString: string) => {
  if (!dateString) return "N/A";
  return new Date(dateString).toLocaleDateString("vi-VN");
};

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
      toast.success("Đã xoá video");
      setVideoToDelete(null);
    } catch (err) {
      console.error("Delete video failed:", err);
      toast.error("Không xoá được video");
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

  return (
    <div className="border-y">
      <Table className="w-full table-fixed">
        <TableHeader>
          <TableRow>
            <TableHead className="pl-6">Video</TableHead>
            <TableHead className="w-[140px] text-center">Hiển thị</TableHead>
            <TableHead className="w-[130px] text-center">Ngày tạo</TableHead>
            <TableHead className="w-[130px] text-center">Cập nhật</TableHead>
            <TableHead className="w-[90px] text-center">Lượt xem</TableHead>
            <TableHead className="w-[90px] text-center" />
          </TableRow>
        </TableHeader>
        <TableBody>
          {/* {videos.map((video) => ( */}
          {sortedVideos.map((video) => (
            <TableRow
              className="cursor-pointer hover:bg-muted/50"
              key={video.videoId}
            >
              <TableCell className="pl-6">
                <Link
                  href={`/studio/videos/${video.videoId}`}
                  className="flex items-center gap-4"
                >
                  <div className="relative aspect-video w-36 shrink-0">
                    <VideoThumbnail
                      imageUrl={video.thumbnailUrl}
                      previewUrl={video.videoUrl}
                      title={video.videoTitle}
                      duration={video.duration}
                    />
                  </div>
                  <div className="flex flex-col overflow-hidden gap-y-1">
                    <span className="text-md font-medium line-clamp-1">
                      {video.videoTitle}
                    </span>
                    <span className="text-sm text-muted-foreground line-clamp-1">
                      {video.description || "No description"}
                    </span>
                  </div>
                </Link>
              </TableCell>
              <TableCell className="text-center">
                {(() => {
                  const badge = STATUS_LABELS[video.videoStatus ?? "ACTIVE"] ?? STATUS_LABELS.ACTIVE;
                  return (
                    <span className={`inline-block rounded-full px-2 py-0.5 text-xs font-medium ${badge.className}`}>
                      {badge.label}
                    </span>
                  );
                })()}
              </TableCell>
              <TableCell className="text-center">
                {formatDate(video.createdAt)}
              </TableCell>
              <TableCell className="text-center">
                {formatDate(video.updatedAt)}
              </TableCell>
              <TableCell className="text-center">{video.totalView}</TableCell>
              <TableCell className="text-center">
                <Button
                  type="button"
                  variant="ghost"
                  size="sm"
                  className="text-red-600 hover:text-red-700"
                  onClick={() => setVideoToDelete(video)}
                >
                  Xoá
                </Button>
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>

      <Dialog open={!!videoToDelete} onOpenChange={(open) => !open && !isDeleting && setVideoToDelete(null)}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Xoá video này?</DialogTitle>
            <DialogDescription>
              &quot;{videoToDelete?.videoTitle}&quot; sẽ bị xoá vĩnh viễn khỏi WeTube, kể cả tệp video và ảnh thu nhỏ.
              Thao tác này không thể hoàn tác.
            </DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <Button variant="ghost" onClick={() => setVideoToDelete(null)} disabled={isDeleting}>
              Huỷ
            </Button>
            <Button variant="destructive" onClick={handleConfirmDelete} disabled={isDeleting}>
              {isDeleting ? "Đang xoá..." : "Xoá"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}

const VideosSectionSkeleton = () => {
  return (
    <div className="border-y">
      <Table>
        <TableHeader>
          <TableRow>
            <TableHead className="pl-6 w-[480px]">Video</TableHead>
            <TableHead className="text-center">Ngày tạo</TableHead>
            <TableHead className="text-center">Cập nhật</TableHead>
            <TableHead className="text-center">Lượt xem</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {Array.from({ length: 5 }).map((_, index) => (
            <TableRow key={index}>
              <TableCell className="pl-6">
                <div className="flex items-center gap-4">
                  <Skeleton className="h-20 w-36" />
                  <div className="flex flex-col gap-2">
                    <Skeleton className="h-4 w-[200px]" />
                    <Skeleton className="h-4 w-[160px]" />
                  </div>
                </div>
              </TableCell>
              <TableCell>
                <Skeleton className="h-4 w-20" />
              </TableCell>
              <TableCell>
                <Skeleton className="h-4 w-16" />
              </TableCell>
              <TableCell>
                <Skeleton className="h-4 w-24" />
              </TableCell>
              <TableCell className="text-right">
                <Skeleton className="h-4 w-12" />
              </TableCell>
              <TableCell className="text-right">
                <Skeleton className="h-4 w-12" />
              </TableCell>
              <TableCell className="text-right">
                <Skeleton className="h-4 w-12" />
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </div>
  );
};
