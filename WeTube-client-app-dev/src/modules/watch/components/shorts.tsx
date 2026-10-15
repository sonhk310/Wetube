import ShortVideo from "./short-video";
import { useVideoStore } from "@/store/zustand/videoStore";
import { useState } from "react";
import { ChevronLeftIcon, ChevronRightIcon } from "@heroicons/react/24/solid";

export default function Shorts() {
    const videoDetail = useVideoStore((s) => s.videoDetail);
    const [page, setPage] = useState(0);
    const itemsPerPage = 3;

    const shorts =
        videoDetail?.recommend?.video.map((v) => ({
            id: v.id,
            title: v.title,
            thumbnail: v.thumbnailUrl,
            views: v.totalView,
        })) ?? [];

    if (!shorts.length) return null;

    const totalPages = Math.ceil(shorts.length / itemsPerPage);
    const startIndex = page * itemsPerPage;
    const currentShorts = shorts.slice(startIndex, startIndex + itemsPerPage);

    const handlePrev = () => {
        if (page > 0) setPage((p) => p - 1);
    };

    const handleNext = () => {
        if (page < totalPages - 1) setPage((p) => p + 1);
    };

        return (
            <section className="flex flex-col gap-2">
                <p className="text-lg font-bold">Shorts</p>
                <div className="relative flex items-center">
                    {page > 0 && (
                        <button
                            onClick={handlePrev}
                            className="absolute -left-6 z-10 bg-white shadow rounded-full p-2"
                        >
                            <ChevronLeftIcon className="size-6" />
                        </button>
                    )}

                    <section className="grid grid-cols-3 gap-2 w-full">
                        {currentShorts.map((item) => (
                            <ShortVideo key={item.id} short={item} />
                        ))}
                    </section>

                    {page < totalPages - 1 && (
                        <button
                            onClick={handleNext}
                            className="absolute -right-6 z-10 bg-white shadow rounded-full p-2"
                        >
                            <ChevronRightIcon className="size-6" />
                        </button>
                    )}
                </div>
            </section>
        );
}
