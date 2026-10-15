"use client";

import { formatDuration } from "@/lib/utils";

interface VideoOverlayProps {
    duration: number;
    progress?: number;
}

const VideoOverlay: React.FC<VideoOverlayProps> = ({ duration, progress }) => {
    return (
        <>
            {/* Duration badge */}
            <span className="absolute bottom-1.25 right-1 bg-black/60 text-white font-semibold text-[13px] px-1 py-[1px] rounded">
        {formatDuration(duration)}
      </span>

            {/* Progress bar */}
            {typeof progress === "number" && progress > 0 && (
                <div className="absolute bottom-[0.1px] w-full h-1 bg-gray-300 rounded-b-4xl overflow-hidden group-hover:hidden">
                    <div
                        className="h-full bg-red-600"
                        style={{ width: `${Math.min(progress * 100, 100)}%` }}
                    />
                </div>
            )}
        </>
    );
};

export default VideoOverlay;
