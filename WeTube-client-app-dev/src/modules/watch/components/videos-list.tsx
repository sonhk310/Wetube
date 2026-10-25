
import VideoPreview from "./video-preview";
import { motion, Variants } from "motion/react";
import { useVideoStore } from "@/store/zustand/videoStore";

const container: Variants = {
  hidden: { opacity: 0 },
  show: {
    opacity: 1,
    transition: {
      duration: 0.15,
      type: "spring",
      staggerChildren: 0.1,
      bounce: 0.2,
    },
  },
};

export default function VideosList() {
    const videoDetail = useVideoStore((s) => s.videoDetail);
    // Shorts đã có mục riêng ở trên
    const videos = videoDetail?.recommend?.video?.filter((v) => !v.isShort) ?? [];

    if (!videos.length) return null;
    return (
        <motion.section
            variants={container}
            initial="hidden"
            animate="show"
            className="flex flex-col gap-1 w-full border-t pt-2 border-gray-200 mb-[58px]"
        >
            {videos.map((video) => (
                <VideoPreview key={video.id} video={video} />
            ))}
        </motion.section>
    );
}
