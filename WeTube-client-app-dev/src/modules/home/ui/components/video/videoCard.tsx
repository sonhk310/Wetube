"use client";

import { useSaveInteractionMutation } from "@/app/api/interactionApi";
import { Skeleton } from '@/components/ui/skeleton';
import VideoOverlay from "@/components/videos/VideoOverlayProps";
import { useAuth } from "@/contexts/auth-context";
import { VideoThumbnail } from '@/modules/videos/ui/components/video-thumbnail';
import { playlistService } from "@/modules/playlists/ui/list/playlist-API";
import { RecommendedVideoItem } from "@/types/video";
import Image from "next/image";
import { useRouter } from "next/navigation";
import { formatViews, timeAgo } from "@/lib/utils";

const VideoCard = ({
  id,
  title,
  thumbnailUrl,
  videoUrl,
  totalView,
  createAt,
  name,
  duration,
  picture,
  historyDuration,
}: RecommendedVideoItem) => {
  const router = useRouter();
  const displayTime = timeAgo(createAt);
  const { user } = useAuth();

  const [saveInteraction] = useSaveInteractionMutation();

  const handleClick = async () => {
    try {
      if (user?.sub && id) {
        await playlistService.addToHistory(user.sub, id);
        console.log("Đã lưu vào history:", id);

        await saveInteraction({
          userId: user.sub, 
          videoId: id,
          type: "VIEW",
        }).unwrap();
      }
    } catch (err) {
      console.error("Failed to record VIEW interaction:", err);
      console.error("Lỗi khi lưu history/interaction:", err);
      console.error("Ghi nhận VIEW thất bại:", err);
    } finally {
      router.push(`/watch/${id}`);
    }
  };

  return (
    <div
      className="w-full flex flex-col cursor-pointer group"
      onClick={handleClick}
    >
      <div className="rounded-xl overflow-hidden transform transition duration-300 group-hover:scale-105 group-hover:shadow-xl group-hover:rounded-none">
        <VideoThumbnail
          imageUrl={thumbnailUrl}
          previewUrl={videoUrl}
          title={title}
          duration={duration}
        />
        {historyDuration &&
          historyDuration > 0 &&
          historyDuration < duration && (
            <VideoOverlay
              duration={duration}
              progress={historyDuration / duration}
            />
          )}
      </div>

      <div className="flex gap-3 pt-3 pr-3 pb-3 pl-0 items-start">
        <Image
          src={picture}
          alt={name}
          width={48}
          height={48}
          className="w-12 h-12 rounded-full object-cover"
        />
        <div>
          <h3 className="text-md font-semibold leading-tight text-black group-hover:text-blue-600 transition-colors line-clamp-2">
            {title}
          </h3>
          <p className="text-sm text-gray-600">{name}</p>
          <p className="text-sm text-gray-400">
            {formatViews(totalView)} • {displayTime}
          </p>
        </div>
      </div>
    </div>
  );
};

export const VideoCardSkeleton = () => {
  return (
    <div className="w-full flex flex-col">
      <Skeleton className="w-full aspect-video rounded-xl" />
      <div className="flex gap-3 pt-3">
        <Skeleton className="w-12 h-12 rounded-full shrink-0" />
        <div className="flex-1 space-y-2">
          <Skeleton className="h-5 w-full" />
          <Skeleton className="h-5 w-3/4" />
          <Skeleton className="h-4 w-1/2" />
        </div>
      </div>
    </div>
  );
};

export default VideoCard;
