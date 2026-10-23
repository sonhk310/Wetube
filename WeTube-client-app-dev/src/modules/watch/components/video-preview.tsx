import { CheckCircleIcon } from "@heroicons/react/24/solid";
import { motion } from "motion/react";
import { useRouter } from "next/navigation";
import { RecommendVideoDto } from "@/types/video";
import { formatViews, timeAgo } from "@/lib/utils";
import { useWatchProgress } from "@/lib/watchProgress";

const item = {
  hidden: { opacity: 0, scale: 0.9 },
  show: { opacity: 1, scale: 1 },
};

export default function VideoPreview({ video }: { video: RecommendVideoDto }) {
  const router = useRouter();
  const watched = useWatchProgress(video.id) ?? 0;
  const handleClick = () => {
    router.push(`/watch/${video.id}`);
  };

  return (
    <motion.div
      variants={item}
      className="grid grid-cols-11 gap-2 mb-2"
    >
      <div
        className="col-span-5 rounded-lg bg-no-repeat bg-cover aspect-video relative cursor-pointer overflow-hidden"
        style={{ backgroundImage: `url(${video.thumbnailUrl})` }}
        onClick={handleClick}
      >
        <p
          style={{ fontSize: "11px" }}
          className="absolute right-1.5 bottom-1.5 rounded-sm bg-gray-800/85 text-white font-semibold px-1"
        >
          {formatDuration(video.duration)}
        </p>
        {watched > 0 && video.duration > 0 && (
          <div className="absolute inset-x-0 bottom-0 h-1 bg-gray-300/80">
            <div
              className="h-full bg-red-600"
              style={{ width: `${Math.min(100, (watched / video.duration) * 100)}%` }}
            />
          </div>
        )}
      </div>
      <div className="col-span-6 flex flex-col justify-start gap-0.5">
        <p className="w-full text-sm font-bold overflow-hidden text-ellipsis line-clamp-2">
          {video.title}
        </p>
        <span className="flex justify-start items-center gap-1">
          <p className="text-sm font-semibold text-gray-500">{video.name}</p>
                    {/* giả sử tất cả đều verified */}
                    <CheckCircleIcon className="size-4 text-gray-500" />
        </span>
        <p className="flex text-xs justify-start items-center gap-2">
          {formatViews(video.totalView)} • {timeAgo(video.createAt)}
        </p>
      </div>
    </motion.div>
  );
}

function formatDuration(seconds: number) {
  const mins = Math.floor(seconds / 60);
  const secs = seconds % 60;
  return `${mins}:${secs.toString().padStart(2, "0")}`;
}
