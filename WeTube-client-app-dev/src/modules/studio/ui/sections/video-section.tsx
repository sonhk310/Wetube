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
import React, { useEffect } from "react";
import { useAuth } from "@/contexts/auth-context";
import {
  useGetPlaylistDetailsQuery,
  useGetPlaylistsByUserIdQuery,
} from "@/app/api/playlistApi";
import { VideoFromPlaylist } from "@/types/playlistSummary";

const formatDate = (dateString: string) => {
  if (!dateString) return "N/A";
  return new Date(dateString).toLocaleDateString("vi-VN");
};

export default function VideosSection() {
  const { user } = useAuth();

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
        Failed to load videos. Please try again later.
      </div>
    );
  }

  if (videosFromPlaylist.length === 0) {
    return (
      <div className="p-6 text-center text-muted-foreground">
        You have no uploaded videos.
      </div>
    );
  }

  return (
    <div className="border-y">
      <Table className="w-full table-fixed">
        <TableHeader>
          <TableRow>
            <TableHead className="pl-6">Video</TableHead>
            <TableHead className="w-[150px] text-center">Created at</TableHead>
            <TableHead className="w-[150px] text-center">Updated at</TableHead>
            <TableHead className="w-[100px] text-center">Views</TableHead>
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
                {formatDate(video.createdAt)}
              </TableCell>
              <TableCell className="text-center">
                {formatDate(video.updatedAt)}
              </TableCell>
              <TableCell className="text-center">{video.totalView}</TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
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
            <TableHead className="text-center">Created at</TableHead>
            <TableHead className="text-center">Updated at</TableHead>
            <TableHead className="text-center">Views</TableHead>
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
